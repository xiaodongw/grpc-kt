package io.grpc.kt

import io.grpc.StatusRuntimeException
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ErrorPropagationTest : TestBase() {
  override fun newService(): EchoGrpcKt.EchoImplBase {
    return object : EchoGrpcKt.EchoImplBase() {
      override suspend fun unary(req: EchoService.EchoReq): EchoService.EchoResp {
        throw RuntimeException("mocked unary error")
      }

      override suspend fun serverStreaming(req: EchoService.EchoCountReq): ReceiveChannel<EchoService.EchoResp> {
        return GlobalScope.produce {
          for(i in 0 until 5) {
            send(EchoService.EchoResp.newBuilder().build())
          }

          throw RuntimeException("mocked server streaming error")
        }
      }

      override suspend fun clientStreaming(req: ReceiveChannel<EchoService.EchoReq>): EchoService.EchoCountResp {
        var count = 0
        for(msg in req) {
          count++
          if (count >= 5) throw RuntimeException("mocked client streaming error")
        }

        return EchoService.EchoCountResp.newBuilder().setCount(count).build()
      }
    }
  }

  @Test
  fun testUnaryError() {
    assertThrows<StatusRuntimeException>("UNKNOWN: mocked unary error") {
      runBlockingWithTimeout(timeout) {
        stub.unary(EchoService.EchoReq.getDefaultInstance())
      }
    }
  }

  @Test
  fun testServerStreamingError() {
    assertThrows<StatusRuntimeException>("UNKNOWN: mocked server streaming error") {
      runBlockingWithTimeout(timeout) {
        stub.serverStreaming(EchoService.EchoCountReq.getDefaultInstance())
      }
    }
  }

  @Test
  fun testClientStreamingError() {
    assertThrows<StatusRuntimeException>("CANCELLED: Subscription cancelled") {
      val req = makeChannel()
      runBlockingWithTimeout(timeout) {
        stub.clientStreaming(req)
      }
    }
  }
}