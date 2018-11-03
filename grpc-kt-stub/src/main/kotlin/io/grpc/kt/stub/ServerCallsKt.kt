package io.grpc.kt.stub

import io.grpc.*
import io.grpc.kt.core.LogUtils
import org.slf4j.LoggerFactory

import java.util.concurrent.CancellationException

import com.google.common.base.Preconditions.checkNotNull
import io.grpc.internal.SerializingExecutor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Utility functions for adapting [ServerCallHandler]s to application service implementation,
 * meant to be used by the generated code.
 */
object ServerCallsKt {

  /**
   * Creates a `ServerCallHandler` for a unary call method of the service.
   *
   * @param method an adaptor to the actual method on the service implementation.
   */
  fun <REQ, RESP> unaryCall(
    method: UnaryMethod<REQ, RESP>): ServerCallHandler<REQ, RESP> {
    return UnaryServerCallHandler(method)
  }

  /**
   * Creates a `ServerCallHandler` for a server streaming method of the service.
   *
   * @param method an adaptor to the actual method on the service implementation.
   */
  fun <REQ, RESP> serverStreamingCall(
    method: ServerStreamingMethod<REQ, RESP>): ServerCallHandler<REQ, RESP> {
    return ServerStreamingServerCallHandler(method)
  }

  /**
   * Creates a `ServerCallHandler` for a client streaming method of the service.
   *
   * @param method an adaptor to the actual method on the service implementation.
   */
  fun <REQ, RESP> clientStreamingCall(
    method: ClientStreamingMethod<REQ, RESP>): ServerCallHandler<REQ, RESP> {
    return ClientStreamingServerCallHandler(method)
  }

  fun <REQ, RESP> bidiStreamingCall(
    method: BidiStreamingMethod<REQ, RESP>): ServerCallHandler<REQ, RESP> {
    return BidiStreamingServerCallHandler(method)
  }

  /**
   * A [SingleObserver] which dispatches the message to GRPC as response.
   *
   * @param <RESP>
  </RESP> */
  private class SingleResponseSender<RESP>(private val call: ServerCall<*, RESP>, private val resp: RESP) : CallHandler {
    companion object {
      private val logger = LoggerFactory.getLogger(SingleResponseSender::class.java)
    }
    override fun start() {
      logger.trace("${call.methodDescriptor.fullMethodName} start")
      call.sendHeaders(Metadata())
      call.sendMessage(resp)
      call.close(Status.OK, Metadata())
    }
  }

  private open class SingleRequestReceiver<REQ>(protected var call: ServerCall<REQ, *>) : ServerCall.Listener<REQ>(), CallHandler {
    companion object {
      private val logger = LoggerFactory.getLogger(SingleRequestReceiver::class.java)
    }
    private var value: REQ? = null
    private val deferred: CompletableDeferred<REQ> = CompletableDeferred()

    var readyHandler: ReadyHandler? = null

    fun value(): Deferred<REQ> {
      return deferred
    }

    override fun onMessage(msg: REQ) {
      logger.debug("${call.methodDescriptor.fullMethodName} onMessage msg=${LogUtils.objectString(msg)}")
      if (value != null) {
        throw Status.INTERNAL.withDescription("More than one value received for unary call")
          .asRuntimeException()
      }
      value = msg
    }

    override fun onCancel() {
      logger.debug("${call.methodDescriptor.fullMethodName} onCancel")
      deferred.completeExceptionally(CancellationException("Request is cancelled"))
    }

    override fun onHalfClose() {
      logger.trace("${call.methodDescriptor.fullMethodName} onHalfClose")
      if (value == null) {
        // No value received so mark the future as an error
        val error = Status.INTERNAL.withDescription("No value received for unary call")
          .asRuntimeException()
        deferred.completeExceptionally(error)
      } else {
        deferred.complete(value!!)
      }
    }

    override fun start() {
      logger.trace("${call.methodDescriptor.fullMethodName} start")
      call.request(2)
    }

    override fun onReady() {
      logger.trace("${call.methodDescriptor.fullMethodName} onReady")
      readyHandler?.onReady()
    }
  }

  /**
   * A [Subscriber] which dispatches messages to GRPC as response.
   *
   * @param <RESP>
  </RESP> */
  internal class StreamResponseSender<RESP>(private val call: ServerCall<*, RESP>,
                                            private val channel: ReceiveChannel<RESP>,
                                            private val dispatcher: CoroutineDispatcher): CallHandler, ReadyHandler {
    companion object {
      private val logger = LoggerFactory.getLogger(StreamResponseSender::class.java)
    }
    private val working = AtomicBoolean(false)

    override fun onReady() {
      logger.trace("onReady")

      kickoff()
    }

    override fun start() {
      call.sendHeaders(Metadata())
      kickoff()
    }

    private fun kickoff() {
      if(!working.compareAndSet(false, true)) return

      logger.trace("kickoff")
      launch(dispatcher) {
        try {
          val msg = channel.receive()
          logger.trace("channel.receive() msg=${LogUtils.objectString(msg)}")
          call.sendMessage(msg)
        } catch (t: Throwable) {
          when(t) {
            is ClosedReceiveChannelException -> call.close(Status.OK, Metadata())
            else -> call.close(getStatus(t), Metadata())
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

  private open class StreamRequestReceiver<REQ>(private val call: ServerCall<REQ, *>,
                                                private val dispatcher: CoroutineDispatcher) : ServerCall.Listener<REQ>(), CallHandler {
    companion object {
      private val logger = LoggerFactory.getLogger(StreamRequestReceiver::class.java)
    }
    private val channel = Channel<REQ>()

    var readyHandler: ReadyHandler? = null

    fun channel(): ReceiveChannel<REQ> {
      return channel
    }


    override fun onMessage(msg: REQ) {
      logger.trace("${call.methodDescriptor.fullMethodName} onMessage: msg=${LogUtils.objectString(msg)}")
      launch(dispatcher) {
        try {
          channel.send(msg)
          call.request(1)
        } catch (t: Throwable) {
          call.close(getStatus(t), Metadata())
        }
      }
    }

    override fun onHalfClose() {
      logger.trace("${call.methodDescriptor.fullMethodName} onHalfClose")
      launch(dispatcher) {
        channel.close()
      }

    }

    override fun onCancel() {
      logger.trace("${call.methodDescriptor.fullMethodName} onCancel")
      launch(dispatcher) {
        channel.close(CancellationException("GRPC call is cancelled"))
      }
    }

    override fun start() {
      logger.trace("${call.methodDescriptor.fullMethodName} start")
      call.request(1)
    }

    override fun onReady() {
      logger.trace("${call.methodDescriptor.fullMethodName} onReady")
      readyHandler?.onReady()
    }
  }

  /**
   * Unary call handler, combines SingleRequestListener & ResponseObserver
   *
   * @param <REQ>
   * @param <RESP>
  </RESP></REQ> */
  class UnaryServerCallHandler<REQ, RESP>(private val method: UnaryMethod<REQ, RESP>) : ServerCallHandler<REQ, RESP> {

    override fun startCall(call: ServerCall<REQ, RESP>, headers: Metadata): ServerCall.Listener<REQ> {
      val dispatcher = SerializingExecutor(ForkJoinPool.commonPool()).asCoroutineDispatcher()
      val requestReceiver = SingleRequestReceiver(call)
      launch(dispatcher) {
        try {
          val req = requestReceiver.value().await()
          val resp = method.unaryInvoke(req)
          val respSender = SingleResponseSender(call, resp)
          respSender.start()
        } catch (t: Throwable) {
          call.close(getStatus(t), Metadata())
        }
      }

      requestReceiver.start()
      return requestReceiver
    }
  }

  /**
   * Server streaming call handler, combines SingleRequestListener & ResponseSubscriber
   *
   * @param <REQ>
   * @param <RESP>
  </RESP></REQ> */
  class ServerStreamingServerCallHandler<REQ, RESP>(private val method: ServerStreamingMethod<REQ, RESP>) : ServerCallHandler<REQ, RESP> {

    override fun startCall(call: ServerCall<REQ, RESP>, headers: Metadata): ServerCall.Listener<REQ> {
      val dispatcher = SerializingExecutor(ForkJoinPool.commonPool()).asCoroutineDispatcher()
      val requestReceiver = SingleRequestReceiver(call)
      launch(dispatcher) {
        try {
          val req = requestReceiver.value().await()
          val resp = method.serverStreamingInvoke(req)
          val respSender = StreamResponseSender(call, resp, dispatcher)
          requestReceiver.readyHandler = respSender
          respSender.start()
        } catch (t: Throwable) {
          call.close(getStatus(t), Metadata())
        }
      }

      requestReceiver.start()
      return requestReceiver
    }
  }

  /**
   * Client streaming handler, combines StreamRequestListener & ResponseObserver
   *
   * @param <REQ>
   * @param <RESP>
  </RESP></REQ> */
  class ClientStreamingServerCallHandler<REQ, RESP>(private val method: ClientStreamingMethod<REQ, RESP>) : ServerCallHandler<REQ, RESP> {

    override fun startCall(call: ServerCall<REQ, RESP>, headers: Metadata): ServerCall.Listener<REQ> {
      val dispatcher = SerializingExecutor(ForkJoinPool.commonPool()).asCoroutineDispatcher()
      val requestReceiver = StreamRequestReceiver(call, dispatcher)
      launch(dispatcher) {
        try {
          val req = requestReceiver.channel()
          val resp = method.clientStreamingInvoke(req)
          val respSender = SingleResponseSender(call, resp)
          respSender.start()
        } catch (t: Throwable) {
          call.close(getStatus(t), Metadata())
        }
      }

      requestReceiver.start()
      return requestReceiver
    }
  }

  /**
   * Bidi streaming handler, combines StreamRequestListener & ResponseSubscriber
   *
   * @param <REQ>
   * @param <RESP>
  </RESP></REQ> */
  class BidiStreamingServerCallHandler<REQ, RESP>(private val method: BidiStreamingMethod<REQ, RESP>) : ServerCallHandler<REQ, RESP> {

    override fun startCall(call: ServerCall<REQ, RESP>, headers: Metadata): ServerCall.Listener<REQ> {
      val dispatcher = SerializingExecutor(ForkJoinPool.commonPool()).asCoroutineDispatcher()
      val requestReceiver = StreamRequestReceiver(call, dispatcher)
      launch(dispatcher) {
        try {
          val req = requestReceiver.channel()
          val resp = method.bidiStreamingInvoke(req)
          val respSender = StreamResponseSender(call, resp, dispatcher)
          requestReceiver.readyHandler = respSender
          respSender.start()
        } catch (t: Throwable) {
          call.close(getStatus(t), Metadata())
        }
      }

      requestReceiver.start()
      return requestReceiver
    }
  }

  /**
   * Adaptor to a unary call method.
   */
  interface UnaryMethod<REQ, RESP> {
    suspend fun unaryInvoke(req: REQ): RESP
  }

  /**
   * Adaptor to a server streaming method.
   */
  interface ServerStreamingMethod<REQ, RESP> {
    suspend fun serverStreamingInvoke(req: REQ): ReceiveChannel<RESP>
  }

  /**
   * Adaptor to a client streaming method.
   */
  interface ClientStreamingMethod<REQ, RESP> {
    suspend fun clientStreamingInvoke(req: ReceiveChannel<REQ>): RESP
  }

  /**
   * Adaptor to a bi-directional streaming method.
   */
  interface BidiStreamingMethod<REQ, RESP> {
    suspend fun bidiStreamingInvoke(req: ReceiveChannel<REQ>): ReceiveChannel<RESP>
  }

  fun <T> unimplementedUnaryCall(
    methodDescriptor: MethodDescriptor<*, *>): T {
    checkNotNull(methodDescriptor)
    throw Status.UNIMPLEMENTED
      .withDescription("Method ${methodDescriptor.fullMethodName} is unimplemented")
      .asRuntimeException()
  }

  fun <T> unimplementedStreamingCall(methodDescriptor: MethodDescriptor<*, *>): ReceiveChannel<T> {
    checkNotNull(methodDescriptor)
    throw Status.UNIMPLEMENTED
      .withDescription("Method ${methodDescriptor.fullMethodName} is unimplemented")
      .asRuntimeException()
  }

  private fun getStatus(t: Throwable): Status {
    val status = Status.fromThrowable(t)
    return if (status.description == null) {
      status.withDescription(t.message)
    } else {
      status
    }
  }
}
