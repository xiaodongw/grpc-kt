package io.grpc.kt

import io.grpc.ManagedChannel
import io.grpc.Server
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.security.MessageDigest
import java.util.*

/**
 * Tested with 100 * 1000 unary calls
 * grpc-kt: 4s 107ms
 * grpc-java: 3s 768ms
 */

class EchoLoadTest : TestBase() {
  override fun newService(): EchoGrpcKt.EchoImplBase {
    return EchoServiceImpl(0)
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
      GlobalScope.launch {
        for(j in 0 until 1000) {
          val msg = UUID.randomUUID().toString()
          val resp = stub.unary(EchoProto.EchoReq.newBuilder().setId(i).setValue(msg).build())
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
      GlobalScope.launch {
        val reqDigest = MessageDigest.getInstance("MD5")
        val respDigest = MessageDigest.getInstance("MD5")
        val req = produce {
          for(j in 0 until 1000) {
            val value = UUID.randomUUID().toString()
            reqDigest.update(value.toByteArray())
            val msg = EchoProto.EchoReq.newBuilder().setId(j).setValue(value).build()
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

class EchoLoadTestJava : TestBase() {
  protected val jstub by lazy { EchoGrpc.newBlockingStub(channel) }

  override fun newService(): EchoGrpc.EchoImplBase {
    return EchoServiceJavaImpl()
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
      GlobalScope.launch {
        for(j in 0 until 1000) {
          val msg = UUID.randomUUID().toString()
          val resp = jstub.unary(EchoProto.EchoReq.newBuilder().setId(i).setValue(msg).build())
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
}