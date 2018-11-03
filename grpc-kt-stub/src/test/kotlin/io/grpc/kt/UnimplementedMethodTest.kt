package io.grpc.kt

import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UnimplementedMethodTest : TestBase() {
  override fun newService(): EchoGrpcKt.EchoImplBase {
    return object : EchoGrpcKt.EchoImplBase() {

    }
  }

  @Test
  fun testUnimplementedMethod() {
    val exception = assertThrows<StatusRuntimeException> {
      runBlockingWithTimeout(timeout) {
        val stub = EchoGrpcKt.newStub(channel)
        stub.unary(EchoProto.EchoReq.getDefaultInstance())
      }
    }
    assertEquals(Status.UNIMPLEMENTED.code, exception.status.code)
    assertEquals("UNIMPLEMENTED: Method io.grpc.kt.Echo/unary is unimplemented", exception.message)
  }
}
