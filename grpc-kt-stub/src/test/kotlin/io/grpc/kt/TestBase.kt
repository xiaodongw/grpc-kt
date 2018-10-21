package io.grpc.kt

import io.grpc.ManagedChannel
import io.grpc.Server
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll

object TestHelper {
  suspend fun <T> consume(channel: ReceiveChannel<T>) {
    for(m in channel) {
      // do nothing
    }
  }

  fun makeChannel(num: Int): ReceiveChannel<EchoService.EchoReq> {
    return GlobalScope.produce {
      for (x in 0 until num) {
        val req = EchoService.EchoReq.newBuilder().setId(x).setValue("Hello").build()
        send(req)
      }
    }
  }
}

abstract class TestBase {
  private val uniqueServerName = "in-process server for $javaClass"
  protected lateinit var channel: ManagedChannel
  private lateinit var server: Server
  protected val timeout: Long = 1000 * 1000
  protected val streamNum = 128
  protected val stub by lazy { EchoGrpcKt.newStub(channel) }

  protected open fun newChannel(): ManagedChannel {
    return InProcessChannelBuilder
      .forName(uniqueServerName)
      .usePlaintext()
      .build()
  }

  protected open fun newServer(): Server {
    return InProcessServerBuilder.forName(uniqueServerName)
      .addService(newService())
      .directExecutor()
      .build()
  }

  protected abstract fun newService(): EchoGrpcKt.EchoImplBase

  @BeforeAll
  @Throws(Exception::class)
  fun setUp() {
    server = newServer()
    server.start()

    channel = newChannel()
  }

  @AfterAll
  fun tearDown() {
    channel.shutdownNow()
    server.shutdownNow()
  }
}
