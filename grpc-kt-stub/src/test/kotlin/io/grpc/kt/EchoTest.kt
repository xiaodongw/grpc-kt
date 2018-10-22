package io.grpc.kt

import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.kt.EchoGrpcKt.EchoImplBase
import io.grpc.kt.EchoService.*
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
import io.grpc.kt.TestHelper.makeChannel
import io.grpc.kt.core.LogUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

abstract class EchoTest : TestBase() {
  private val logger = LoggerFactory.getLogger(this.javaClass)

  override fun newService(): EchoImplBase {
    return object : EchoImplBase() {
      override suspend fun unary(req: EchoReq): EchoResp {
        logger.debug("unary req=${LogUtils.objectString(req)} id=${req.id}")
        val resp = EchoResp.newBuilder()
          .setId(req.id)
          .setValue(req.value)
          .build()
        logger.debug("unary resp=${LogUtils.objectString(resp)} id=${resp.id}")
        return resp
      }

      override suspend fun serverStreaming(req: EchoCountReq): ReceiveChannel<EchoResp> {
        logger.debug("serverStreaming req=${LogUtils.objectString(req)} count=${req.count}")
        return GlobalScope.produce {
          for (i in 0 until req.count) {
            val msg = EchoResp.newBuilder()
              .setId(i)
              .setValue(i.toString())
              .build()
            logger.debug("serverStreaming resp.msg=${LogUtils.objectString(msg)} id=${msg.id}")
            send(msg)
          }
        }
      }

      override suspend fun clientStreaming(req: ReceiveChannel<EchoReq>): EchoCountResp {
        var count = 0
        for(msg in req) {
          logger.debug("clientStreaming req.msg=${LogUtils.objectString(msg)} id=${msg.id}")
          require(msg.id == count)
          count++
        }

        val resp = EchoCountResp.newBuilder()
          .setCount(count)
          .build()
        logger.debug("clientStreaming resp=${LogUtils.objectString(resp)} count=${resp.count}")
        return resp
      }

      override suspend fun bidiStreaming(req: ReceiveChannel<EchoReq>): ReceiveChannel<EchoResp> {
        return GlobalScope.produce {
          var count = 0

          for(reqMsg in req) {
            require(reqMsg.id == count)
            count++

            logger.debug("bidiStreaming req.msg=${LogUtils.objectString(reqMsg)} id=${reqMsg.id}")
            val respMsg = EchoResp.newBuilder()
              .setId(reqMsg.id)
              .setValue(reqMsg.value)
              .build()
            logger.debug("bidiStreaming resp.msg=${LogUtils.objectString(respMsg)} id=${respMsg.id}")
            send(respMsg)
          }
        }
      }
    }
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
