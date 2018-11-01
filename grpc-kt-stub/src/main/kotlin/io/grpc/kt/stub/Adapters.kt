package io.grpc.kt.stub

import io.grpc.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

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

internal fun <T> launch(cd: CoroutineDispatcher, block: suspend () -> T): Job {
  val ctx = Context.current()
  return GlobalScope.launch(cd) {
    val prev = ctx.attach()
    try {
      block()
    } finally {
      ctx.detach(prev)
    }
  }
}