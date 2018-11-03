package io.grpc.kt

import io.grpc.ManagedChannel
import io.grpc.Server
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.ForkJoinPool

class EchoLoadTest : TestBase() {
  private val dispatcher = ForkJoinPool.commonPool().asCoroutineDispatcher()

  override fun newService(): EchoGrpcKt.EchoImplBase {
    return EchoServiceImpl(1)
  }

  override fun newChannel(): ManagedChannel {
    return IntegrationTestHelper.newChannel()
  }

  override fun newServer(): Server {
    return IntegrationTestHelper.newServer(newService())
  }

  @Test
  fun testUnaryWithLoad() {
    val jobs = (0 until 100).map { i ->
      GlobalScope.launch(dispatcher) {
        for(j in 0 until 1000) {
          val msg = UUID.randomUUID().toString()
          val resp = stub.unary(EchoService.EchoReq.newBuilder().setId(i).setValue(msg).build())
          assertEquals(i, resp.id)
          assertEquals(msg, resp.value)
        }
      }
    }

    runBlocking {
      jobs.joinAll()
    }
    jobs.forEach {
      assertFalse(it.isCancelled)
    }
  }

  @Test
  fun testStreamingWithLoad() {
    val jobs = (0 until 100).map { i ->
      GlobalScope.launch(dispatcher) {
        val reqDigest = MessageDigest.getInstance("MD5")
        val respDigest = MessageDigest.getInstance("MD5")
        val req = produce {
          for(j in 0 until 1000) {
            val value = UUID.randomUUID().toString()
            reqDigest.update(value.toByteArray())
            val msg = EchoService.EchoReq.newBuilder().setId(j).setValue(value).build()
            send(msg)
          }
        }

        val resp = stub.bidiStreaming(req)
        for(msg in resp) {
          respDigest.update(msg.value.toByteArray())
        }

        val reqHash = reqDigest.digest()
        val respHash = respDigest.digest()
        assertArrayEquals(reqHash, respHash)
      }
    }

    runBlocking {
      jobs.joinAll()
    }
    jobs.forEach {
      assertFalse(it.isCancelled)
    }
  }
}