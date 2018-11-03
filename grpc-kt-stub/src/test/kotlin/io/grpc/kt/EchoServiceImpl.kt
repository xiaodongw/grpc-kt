package io.grpc.kt

import io.grpc.kt.core.LogUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory

class EchoServiceImpl(private val lag: Long = 0) : EchoGrpcKt.EchoImplBase() {
  private val logger = LoggerFactory.getLogger(this.javaClass)
  override suspend fun unary(req: EchoService.EchoReq): EchoService.EchoResp {
    logger.debug("unary req=${LogUtils.objectString(req)} id=${req.id}")
    if (lag > 0) {
      delay(lag)
    }
    val resp = EchoService.EchoResp.newBuilder()
      .setId(req.id)
      .setValue(req.value)
      .build()
    logger.debug("unary resp=${LogUtils.objectString(resp)} id=${resp.id}")
    return resp
  }

  override suspend fun serverStreaming(req: EchoService.EchoCountReq): ReceiveChannel<EchoService.EchoResp> {
    logger.debug("serverStreaming req=${LogUtils.objectString(req)} count=${req.count}")
    if (lag > 0) {
      delay(lag)
    }
    return GlobalScope.produce {
      for (i in 0 until req.count) {
        val msg = EchoService.EchoResp.newBuilder()
          .setId(i)
          .setValue(i.toString())
          .build()
        logger.debug("serverStreaming resp.msg=${LogUtils.objectString(msg)} id=${msg.id}")
        send(msg)
      }
    }
  }

  override suspend fun clientStreaming(req: ReceiveChannel<EchoService.EchoReq>): EchoService.EchoCountResp {
    var count = 0
    for (msg in req) {
      if (lag > 0) {
        delay(lag)
      }
      logger.debug("clientStreaming req.msg=${LogUtils.objectString(msg)} id=${msg.id}")
      require(msg.id == count)
      count++
    }

    val resp = EchoService.EchoCountResp.newBuilder()
      .setCount(count)
      .build()
    logger.debug("clientStreaming resp=${LogUtils.objectString(resp)} count=${resp.count}")
    return resp
  }

  override suspend fun bidiStreaming(req: ReceiveChannel<EchoService.EchoReq>): ReceiveChannel<EchoService.EchoResp> {
    return GlobalScope.produce {
      var count = 0

      for (reqMsg in req) {
        if (lag > 0) {
          delay(lag)
        }
        require(reqMsg.id == count)
        count++

        logger.debug("bidiStreaming req.msg=${LogUtils.objectString(reqMsg)} id=${reqMsg.id}")
        val respMsg = EchoService.EchoResp.newBuilder()
          .setId(reqMsg.id)
          .setValue(reqMsg.value)
          .build()
        logger.debug("bidiStreaming resp.msg=${LogUtils.objectString(respMsg)} id=${respMsg.id}")
        send(respMsg)
      }
    }
  }
}