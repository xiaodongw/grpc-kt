package io.grpc.kt.stub

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