package io.grpc.kt

import io.grpc.StatusRuntimeException
import io.grpc.kt.EchoService.EchoReq
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
import io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NonExistServiceTest {
  private val timeout: Long = 1000
  private val client: EchoGrpcKt.EchoStub = newClient()

  @Test
  fun testUnaryCall() {
    assertThrows<StatusRuntimeException> {
      runBlockingWithTimeout(timeout) {
        client.unary(EchoReq.getDefaultInstance())
      }
    }
  }

  @Test
  fun testBidiStreamingCall() {
    assertThrows<StatusRuntimeException> {
      runBlockingWithTimeout(timeout) {
        client.bidiStreaming(Channel())
      }
    }
  }

  private fun newClient(): EchoGrpcKt.EchoStub {
    val channel = NettyChannelBuilder
      .forAddress("abc", 123)
      .usePlaintext()
      .build()

    return EchoGrpcKt.newStub(channel)
  }
}
