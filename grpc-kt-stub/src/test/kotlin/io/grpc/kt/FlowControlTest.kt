package io.grpc.kt

import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.kt.EchoService.EchoReq
import io.grpc.kt.EchoService.EchoResp
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
import io.grpc.kt.TestHelper.makeChannel
import io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.NettyServerBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

abstract class FlowControlTest : TestBase() {
  private val logger = LoggerFactory.getLogger(javaClass)
  private val bigStr = String(ByteArray(1024 * 8))


  override fun newService(): EchoGrpcKt.EchoImplBase {
    return object : EchoGrpcKt.EchoImplBase() {
      override suspend fun bidiStreaming(req: ReceiveChannel<EchoReq>): ReceiveChannel<EchoResp> {
        return GlobalScope.produce {
          for (reqMsg in req) {
            logger.debug("Received req.msg=${reqMsg.id}")
            delay(10)

            val respMsg = EchoResp.newBuilder()
              .setId(reqMsg.id)
              .setValue(reqMsg.value)
              .build()
            send(respMsg)
            logger.debug("Sent resp.msg=${respMsg.id}")
          }
        }
      }

      override suspend fun serverStreaming(req: EchoService.EchoCountReq): ReceiveChannel<EchoResp> {
        return GlobalScope.produce {
          for (i in 0 until req.count) {
            val respMsg = EchoResp.newBuilder()
              .setId(i)
              .setValue(bigStr)
              .build()
            send(respMsg)
            logger.debug("Sent resp.msg=${respMsg.id}")
          }
        }
      }
    }
  }

  @Test
  fun testFlowControlSlowServer() {
    runBlockingWithTimeout(timeout) {
      val req = GlobalScope.produce {
        for (x in 0 until streamNum) {
          val req = EchoService.EchoReq.newBuilder()
            .setId(x)
            .setValue(bigStr)
            .build()
          send(req)
          logger.debug("Generated req=${req.id}")
        }
      }
      val resp = stub.bidiStreaming(req)
      for (msg in resp) {
        logger.debug("Received resp.msg=${msg.id}")
      }
    }
  }

  @Test
  fun testFlowControlSlowClient() {
    runBlockingWithTimeout(timeout) {
      val req = EchoService.EchoCountReq.newBuilder().setCount(streamNum).build()
      val resp = stub.serverStreaming(req)
      for (msg in resp) {
        logger.debug("Received resp.msg=${msg.id}")
      }
    }
  }
}

//@Ignore("Flow control test, manually verified by log")
class FlowControlUnitTest : FlowControlTest()

//@Ignore("Flow control test, manually verified by log")
class FlowControlIntegrationTest : FlowControlTest() {
  // tiny window so only few outstanding messages is allowed
  private val flowWindow = 32  // bytes

  override fun newChannel(): ManagedChannel {
    return NettyChannelBuilder
      .forAddress("localhost", 1234)
      .flowControlWindow(flowWindow)
      .usePlaintext()
      .build()
  }

  override fun newServer(): Server {
    return NettyServerBuilder
      .forPort(1234)
      .flowControlWindow(flowWindow)
      .addService(newService())
      .build()
  }
}
