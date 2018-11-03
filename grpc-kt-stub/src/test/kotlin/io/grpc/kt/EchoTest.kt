package io.grpc.kt

import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.kt.EchoService.EchoCountReq
import io.grpc.kt.EchoService.EchoReq
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
import io.grpc.kt.TestHelper.makeChannel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

abstract class EchoTest : TestBase() {
  override fun newService(): EchoGrpcKt.EchoImplBase {
    return EchoServiceImpl()
  }

  @Test
  fun testUnary() {
    runBlockingWithTimeout(timeout) {
      val req = EchoReq.newBuilder()
        .setId(1)
        .setValue("Hello")
        .build()
      val resp = stub.unary(req)
      assertEquals(1, resp.id)
      assertEquals("Hello", resp.value)
    }
  }

  @Test
  fun testServerStreaming() {
    runBlockingWithTimeout(timeout) {
      val req = EchoCountReq.newBuilder()
        .setCount(streamNum)
        .build()

      val resp = stub.serverStreaming(req)
      var count = 0
      for (msg in resp) {
        require(msg.id == count)
        count++
      }
      assertEquals(streamNum, count)
    }
  }

  @Test
  fun testClientStreaming() {
    runBlockingWithTimeout(timeout) {
      val req = makeChannel(streamNum)
      val resp = stub.clientStreaming(req)
      assertEquals(streamNum, resp.count)
    }
  }

  @Test
  fun testBidiStreaming() {
    runBlockingWithTimeout(timeout) {
      val req = makeChannel(streamNum)
      val resp = stub.bidiStreaming(req)
      var count = 0
      for(msg in resp) {
        require(msg.id == count)
        count++
      }

      assertEquals(streamNum, count)
    }
  }
}

class EchoUnitTest : EchoTest()

class EchoIntegrationTest : EchoTest() {
  override fun newChannel(): ManagedChannel {
    return IntegrationTestHelper.newChannel()
  }

  override fun newServer(): Server {
    return IntegrationTestHelper.newServer(newService())
  }
}
