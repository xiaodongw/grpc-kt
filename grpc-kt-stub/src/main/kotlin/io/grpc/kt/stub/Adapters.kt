package io.grpc.kt.stub

import io.grpc.Context
import kotlinx.coroutines.*
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Interface for starting the call.
 * The response Single / Flowable will trigger starting call on subscribe.
 */
interface CallHandler {
  fun start()
}

interface ReadyHandler {
  fun onReady()
}

class GrpcContext(
  val value: Context = Context.current()
): ThreadContextElement<Context>, AbstractCoroutineContextElement(Key) {
  companion object Key : CoroutineContext.Key<GrpcContext>

  override fun updateThreadContext(context: CoroutineContext): Context {
    return value.attach()
  }

  override fun restoreThreadContext(context: CoroutineContext, oldState: Context) {
    value.detach(oldState)
  }
}

internal fun launchGrpc(cd: CoroutineDispatcher, block: suspend CoroutineScope.() -> Unit): Job {
  return GlobalScope.launch(context = cd + GrpcContext(), block = block)
}