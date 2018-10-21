package io.grpc.kt

import io.grpc.StatusRuntimeException
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnimplementedMethodTest : TestBase() {
  override fun newService(): EchoGrpcKt.EchoImplBase {
    return object : EchoGrpcKt.EchoImplBase() {

    }
  }

  @Test
  fun testUnimplementedMethod() {
    assertThrows<StatusRuntimeException> {
      runBlockingWithTimeout(timeout) {
        val stub = EchoGrpcKt.newStub(channel)
        stub.unary(EchoService.EchoReq.getDefaultInstance())

      }
    }
  }
}
