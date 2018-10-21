package io.grpc.kt.stub

import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.kt.core.LogUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

object ClientCallsRx {

  /**
   * Executes a unary call with a response.
   */
  suspend fun <REQ, RESP> unaryCall(
    call: ClientCall<REQ, RESP>,
    req: REQ): RESP {
    val requestSender = SingleRequestSender(call, req)
    val responseReceiver = SingleResponseReceiver(call)

    call.start(responseReceiver, Metadata())
    requestSender.start()
    responseReceiver.start()

    return responseReceiver.deferred().await()
  }

  /**
   * Executes a server-streaming call with a response [ReceiveChannel].
   */
  suspend fun <REQ, RESP> serverStreamingCall(
    call: ClientCall<REQ, RESP>,
    req: REQ): ReceiveChannel<RESP> {
    val requestSender = SingleRequestSender(call, req)
    val responseReceiver = StreamResponseReceiver(call)

    call.start(responseReceiver, Metadata())
    requestSender.start()
    responseReceiver.start()

    return responseReceiver.channel()
  }

  /**
   * Executes a client-streaming call by sending a [ReceiveChannel] and returns a [Deferred]
   *
   * @return requestMore stream observer.
   */
  suspend fun <REQ, RESP> clientStreamingCall(
    call: ClientCall<REQ, RESP>,
    reqs: ReceiveChannel<REQ>,
    options: CallOptions): RESP {
    val requestSender = StreamRequestSender(call, reqs)
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
    reqs: ReceiveChannel<REQ>,
    options: CallOptions): ReceiveChannel<RESP> {
    val requestSender = StreamRequestSender(call, reqs)
    val responseReceiver = StreamResponseReceiver(call)
    responseReceiver.readyHandler = requestSender

    call.start(responseReceiver, Metadata())
    requestSender.start()
    responseReceiver.start()

    return responseReceiver.channel()
  }

  private class SingleRequestSender<REQ>(private val call: ClientCall<REQ, *>, private val req: REQ) : CallHandler {

    override fun start() {
      call.sendMessage(req)
      call.halfClose()
    }
  }

  private open class SingleResponseReceiver<RESP>(protected var call: ClientCall<*, RESP>) : ClientCall.Listener<RESP>(), CallHandler {
    private var value: RESP? = null
    private val deferred: CompletableDeferred<RESP> = CompletableDeferred()

    var readyHandler: ReadyHandler? = null

    fun deferred(): Deferred<RESP> {
      return deferred
    }

    override fun onMessage(v: RESP?) {
      if (value != null) {
        throw Status.INTERNAL.withDescription("More than one deferred received for unary call")
          .asRuntimeException()
      }
      value = v
    }

    override fun onClose(status: Status, trailers: Metadata) {
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
      call.request(2)
    }

    override fun onReady() {
      readyHandler?.onReady()
    }
  }

  private class StreamRequestSender<REQ>(private val call: ClientCall<REQ, *>, private val channel: ReceiveChannel<REQ>) : CallHandler, ReadyHandler {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val working = AtomicBoolean(false)

    override fun onReady() {
      logger.trace("onReady")

      kickoff()
    }

    override fun start() {
      kickoff()
    }

    private fun kickoff() {
      if(!working.compareAndSet(false, true)) return

      logger.debug("kickoff")
      GlobalScope.launch {
        try {
          val msg = channel.receive()
          logger.debug("channel.receive() msg=${LogUtils.objectString(msg)}")
          call.sendMessage(msg)
        } catch (t: Throwable) {
          when(t) {
            is ClosedReceiveChannelException -> call.halfClose()
            else -> call.cancel("Error on receiving message from channel", t)
          }
        } finally {
          working.set(false)
        }

        // if grpc is ready to kickoff more
        if (call.isReady) {
          kickoff()
        }
      }
    }
  }

  private open class StreamResponseReceiver<RESP>(private val call: ClientCall<*, RESP>) : ClientCall.Listener<RESP>(), CallHandler {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val channel = Channel<RESP>()

    var readyHandler: ReadyHandler? = null

    fun channel(): ReceiveChannel<RESP> {
      return channel
    }


    override fun onMessage(msg: RESP) {
      logger.trace("onMessage: msg=${LogUtils.objectString(msg)}")
      GlobalScope.launch {
        try {
          channel.send(msg)
          logger.debug("channel.send() msg=${LogUtils.objectString(msg)}")
          call.request(1)
        } catch (t: Throwable) {
          call.cancel("Error dispatching messages", t)
        }
      }
    }

    override fun onClose(status: Status, trailers: Metadata) {
      logger.trace("onClose")
      if (status.isOk) {
        channel.close()
      } else {
        channel.close(status.asRuntimeException(trailers))
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
