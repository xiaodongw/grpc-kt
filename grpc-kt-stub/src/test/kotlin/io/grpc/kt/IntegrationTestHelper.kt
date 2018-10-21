package io.grpc.kt

import io.grpc.BindableService
import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

object IntegrationTestHelper {
  fun newChannel(): ManagedChannel {
    return NettyChannelBuilder
      .forAddress("localhost", 1234)
      .usePlaintext()
      .build()
  }

  fun newServer(service: BindableService): Server {
    return NettyServerBuilder
      .forPort(1234)
      .addService(service)
      .build()
  }

  fun <T> runBlockingWithTimeout(millis: Long, block: suspend CoroutineScope.() -> T) {
    runBlocking { withTimeout(millis, block) }
  }
}
