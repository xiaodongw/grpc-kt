package io.grpc.kt

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
import io.grpc.kt.TestHelper.consume
import io.grpc.netty.NettyChannelBuilder
import kotlinx.coroutines.channels.Channel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NonExistServiceTest {
  private val timeout: Long = 1000
  private val client: EchoGrpcKt.EchoStub = newClient()

  @Test
  fun testUnaryCall() {
    val exception = assertThrows<StatusRuntimeException> {
      runBlockingWithTimeout(timeout) {
        client.unary(EchoProto.EchoReq.getDefaultInstance())
      }
    }
    assertEquals(Status.UNAVAILABLE.code, exception.status.code)
  }

  @Test
  fun testBidiStreamingCall() {
    val exception = assertThrows<StatusRuntimeException> {
      runBlockingWithTimeout(timeout) {
        val req = client.bidiStreaming(Channel())
        consume(req)
      }
    }
    assertEquals(Status.UNAVAILABLE.code, exception.status.code)
  }

  private fun newClient(): EchoGrpcKt.EchoStub {
    val channel = NettyChannelBuilder
      .forAddress("abc", 123)
      .usePlaintext()
      .build()

    return EchoGrpcKt.newStub(channel)
  }
}
