package io.grpc.kt

import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.kt.EchoService.EchoReq
import io.grpc.kt.EchoService.EchoResp
import io.grpc.kt.IntegrationTestHelper.runBlockingWithTimeout
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
            delay(100)

            val respMsg = EchoResp.newBuilder()
              .setId(reqMsg.id)
              .setValue(reqMsg.value)
              .build()
            send(respMsg)
          }
        }
      }
    }
  }

  @Test
  fun testFlowControl() {
    runBlockingWithTimeout(timeout) {
      val req = makeChannel()
      val resp = stub.bidiStreaming(req)
      for (msg in resp) {
        // do nothing
      }
    }
  }

  //@Ignore("Flow control test, manually verified by log")
  class FlowControlUnitTest : FlowControlTest()

  //@Ignore("Flow control test, manually verified by log")
  class FlowControlIntegrationTest : FlowControlTest() {
    // tiny window so only few outstanding messages is allowed
    private val flowWindow = 32

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
}
