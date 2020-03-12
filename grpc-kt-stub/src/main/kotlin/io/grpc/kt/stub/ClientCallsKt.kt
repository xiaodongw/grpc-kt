package io.grpc.kt.stub

import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.internal.SerializingExecutor
import io.grpc.kt.core.LogUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.reactive.asFlow

object ClientCallsKt {
  private val logger = LoggerFactory.getLogger(SingleRequestSender::class.java)

  /**
   * Executes a unary call with a response.
   */
  suspend fun <REQ, RESP> unaryCall(
    call: ClientCall<REQ, RESP>,
    req: REQ): RESP {
    val deferred: CompletableDeferred<RESP> = CompletableDeferred()

    withContext(GrpcContext()) {
      val listener = object: ClientCall.Listener<RESP>() {
        private var value: RESP? = null
        override fun onMessage(msg: RESP) {
          logger.trace("onMessage: msg=${LogUtils.objectString(msg)}")
          if (value != null) {
            throw Status.INTERNAL.withDescription("More than one deferred received for unary call")
              .asRuntimeException()
          }
          value = msg
        }

        override fun onClose(status: Status, trailers: Metadata) {
          logger.trace("onClose: $status")
          if (status.isOk) {
            if (value == null) {
              // No deferred received so mark the future as an error
              val error = Status.INTERNAL.withDescription("No deferred received for unary call")
                .asRuntimeException(trailers)
              deferred.completeExceptionally(error)
            } else {
              deferred.complete(value!!)
            }
          } else {
            deferred.completeExceptionally(status.asRuntimeException(trailers))
          }
        }
      }
      call.start(listener, Metadata())

      call.sendMessage(req)
      call.halfClose()
      call.request(2)
    }

    // cancel the GPRC request if the coroutine is cancelled
    deferred.invokeOnCompletion { cause ->
      cause?.let { call.cancel("Cancelled by client", cause) }
    }

    return deferred.await()
  }

  /**
   * Executes a server-streaming call with a response [Flow].
   */
  suspend fun <REQ, RESP> serverStreamingCall(
    call: ClientCall<REQ, RESP>,
    req: REQ): Flow<RESP> {
    return coroutineScope {
      callbackFlow {
        invokeOnClose { cause ->
          cause?.let { call.cancel("Cancelled by client", cause) }
        }

        val publisher: Publisher<String>? = null

        publisher?.let { it.asFlow() }

        val listener = object: ClientCall.Listener<RESP>() {
          override fun onMessage(message: RESP) {
            // offer(message)
            sendBlocking(message)
          }

          override fun onClose(status: Status, trailers: Metadata) {
            logger.trace("onClose: $status")
            if (status.isOk) {
              close(null)
            } else {
              close(status.asException())
            }
          }
        }

        call.start(listener, Metadata())

        call.sendMessage(req)
        call.halfClose()
        call.request(2)

        awaitClose()
      }
    }
  }

  /**
   * Executes a client-streaming call by sending a [ReceiveChannel] and returns a [Deferred]
   *
   * @return requestMore stream observer.
   */
  suspend fun <REQ, RESP> clientStreamingCall(
    call: ClientCall<REQ, RESP>,
    req: Flow<REQ>): RESP {
    val dispatcher = SerializingExecutor(ForkJoinPool.commonPool()).asCoroutineDispatcher()
    val requestSender = StreamRequestSender(call, req, dispatcher)
    val responseReceiver = SingleResponseReceiver(call)
    responseReceiver.readyHandler = requestSender

    call.start(responseReceiver, Metadata())
    requestSender.start()
    responseReceiver.start()

    return responseReceiver.deferred().await()
  }

  /**
   * Executes a bidi-streaming call.
   *
   * @return requestMore stream observer.
   */
  suspend fun <REQ, RESP> bidiStreamingCall(
    call: ClientCall<REQ, RESP>,
    req: Flow<REQ>): Flow<RESP> {
    val dispatcher = SerializingExecutor(ForkJoinPool.commonPool()).asCoroutineDispatcher()
    val requestSender = StreamRequestSender(call, req, dispatcher)
    val responseReceiver = StreamResponseReceiver(call, dispatcher)
    responseReceiver.readyHandler = requestSender

    call.start(responseReceiver, Metadata())
    requestSender.start()
    responseReceiver.start()

    return responseReceiver.channel()
  }

  private class SingleRequestSender<REQ>(private val call: ClientCall<REQ, *>, private val req: REQ) : CallHandler {
    companion object {
      private val logger = LoggerFactory.getLogger(SingleRequestSender::class.java)
    }

    override fun start() {
      logger.trace("start")
      call.sendMessage(req)
      call.halfClose()
    }
  }

  private open class SingleResponseReceiver<RESP>(protected var call: ClientCall<*, RESP>) : ClientCall.Listener<RESP>(), CallHandler {
    companion object {
      private val logger = LoggerFactory.getLogger(SingleResponseReceiver::class.java)
    }

    private var value: RESP? = null
    private val deferred: CompletableDeferred<RESP> = CompletableDeferred()

    var readyHandler: ReadyHandler? = null

    fun deferred(): Deferred<RESP> {
      return deferred
    }

    override fun onMessage(msg: RESP) {
      logger.trace("onMessage: msg=${LogUtils.objectString(msg)}")
      if (value != null) {
        throw Status.INTERNAL.withDescription("More than one deferred received for unary call")
          .asRuntimeException()
      }
      value = msg
    }

    override fun onClose(status: Status, trailers: Metadata) {
      logger.trace("onClose: $status")
      if (status.isOk) {
        if (value == null) {
          // No deferred received so mark the future as an error
          val error = Status.INTERNAL.withDescription("No deferred received for unary call")
              .asRuntimeException(trailers)
          deferred.completeExceptionally(error)
        } else {
          deferred.complete(value!!)
        }
      } else {
        deferred.completeExceptionally(status.asRuntimeException(trailers))
      }
    }

    override fun start() {
      logger.trace("start")
      call.request(2)
    }

    override fun onReady() {
      logger.trace("onReady")
      readyHandler?.onReady()
    }
  }

  private class StreamRequestSender<REQ>(private val call: ClientCall<REQ, *>,
                                         private val flow: Flow<REQ>,
                                         private val dispatcher: CoroutineDispatcher)
    : CallHandler, ReadyHandler {
    companion object {
      private val logger = LoggerFactory.getLogger(StreamRequestSender::class.java)
    }
    private val working = AtomicBoolean(false)
    private var completed = false
    private val semaphore = Semaphore(1)

    override fun onReady() {
      logger.trace("onReady")

      semaphore.release()
    }

    override fun start() {
      logger.trace("start")
      kickoff()
    }

    private fun kickoff() {
      if (completed) return
      if (!working.compareAndSet(false, true)) return

      logger.trace("kickoff")
      launchGrpc(dispatcher) {
        try {
          flow.collect { msg ->
            logger.trace("flow.collect() msg=${LogUtils.objectString(msg)}")
            call.sendMessage(msg)

            if(!call.isReady) {
              semaphore.acquire()
            }
          }

          call.halfClose()
          completed = true
        } catch (t: Throwable) {
          call.cancel("Error on receiving message from flow", t)
          coroutineContext.cancel(CancellationException(t.message, t))
        } finally {
          working.set(false)
        }
      }
    }
  }

  private open class StreamResponseReceiver<RESP>(private val call: ClientCall<*, RESP>,
                                                  private val dispatcher: CoroutineDispatcher)
    : ClientCall.Listener<RESP>(), CallHandler {
    companion object {
      private val logger = LoggerFactory.getLogger(StreamResponseReceiver::class.java)
    }
    private val channel = Flow<RESP>()

    var readyHandler: ReadyHandler? = null

    fun channel(): Flow<RESP> {
      return channel
    }


    override fun onMessage(msg: RESP) {
      logger.trace("onMessage: msg=${LogUtils.objectString(msg)}")
      launch(dispatcher) {
        try {
          channel.send(msg)
          logger.trace("channel.send() msg=${LogUtils.objectString(msg)}")
          call.request(1)
        } catch (t: Throwable) {
          call.cancel("Error dispatching messages", t)
        }
      }
    }

    override fun onClose(status: Status, trailers: Metadata) {
      logger.trace("onClose")
      launch(dispatcher) {
        if (status.isOk) {
          channel.close()
        } else {
          channel.close(status.asRuntimeException(trailers))
        }
      }
    }

    override fun start() {
      call.request(1)
    }

    override fun onReady() {
      readyHandler?.onReady()
    }
  }
}
