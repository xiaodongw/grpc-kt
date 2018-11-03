package io.grpc.kt

import io.grpc.StatusRuntimeException
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
import io.grpc.kt.TestHelper.consume
import io.grpc.kt.TestHelper.makeChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ErrorPropagationTest : TestBase() {
  override fun newService(): EchoGrpcKt.EchoImplBase {
    return object : EchoGrpcKt.EchoImplBase() {
      override suspend fun unary(req: EchoProto.EchoReq): EchoProto.EchoResp {
        throw RuntimeException("mocked unary error")
      }

      override suspend fun serverStreaming(req: EchoProto.EchoCountReq): ReceiveChannel<EchoProto.EchoResp> {
        return GlobalScope.produce {
          for(i in 0 until 5) {
            send(EchoProto.EchoResp.newBuilder().build())
          }

          throw RuntimeException("mocked server streaming error")
        }
      }

      override suspend fun clientStreaming(req: ReceiveChannel<EchoProto.EchoReq>): EchoProto.EchoCountResp {
        var count = 0
        for(msg in req) {
          count++
          if (count >= 5) throw RuntimeException("mocked client streaming error")
        }

        return EchoProto.EchoCountResp.newBuilder().setCount(count).build()
      }
    }
  }

  @Test
  fun testUnaryError() {
    val exception = assertThrows<StatusRuntimeException> {
      runBlockingWithTimeout(timeout) {
        stub.unary(EchoProto.EchoReq.getDefaultInstance())
      }
    }
    assertEquals("UNKNOWN: mocked unary error", exception.message)
  }

  @Test
  fun testServerStreamingError() {
    val exception = assertThrows<StatusRuntimeException> {
      runBlockingWithTimeout(timeout) {
        val resp = stub.serverStreaming(EchoProto.EchoCountReq.getDefaultInstance())
        consume(resp)
      }
    }
    assertEquals("UNKNOWN: mocked server streaming error", exception.message)
  }

  @Test
  fun testClientStreamingError() {
    val exception = assertThrows<StatusRuntimeException> {
      val req = makeChannel(10)
      runBlockingWithTimeout(timeout) {
        stub.clientStreaming(req)
      }
    }
    assertEquals("UNKNOWN: mocked client streaming error", exception.message)
  }
}