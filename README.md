# GRPC-Kotlin
[![Travis](https://travis-ci.org/xiaodongw/grpc-kt.svg?branch=master)](https://travis-ci.org/xiaodongw/grpc-kt)

This is a GRPC stub & compiler for Kotlin Coroutine. 
By using Kotlin Coroutine, your code is totally asynchronous and non-blocking, 
but still being written in a synchronous way make it easier to read and maintain.

The generated service base and client stub share the same service interface, so they can be easily swapped for dependency injection and test mocking.

## Getting started

* In your project, configure Gradle to use the grpc-kt stub and compiler plugin.
```groovy
dependencies {
  compile "com.github.xiaodongw:grpc-kt-stub:0.3.0"
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:3.5.1"
  }
  plugins {
    grpc {
      artifact = 'com.github.xiaodongw:protoc-gen-grpc-kt:0.3.0'
    }
  }
  generateProtoTasks {
    all()*.plugins {
      grpc {}
    }
  }
}
```

* Server side (See [EchoServiceImpl.kt](grpc-kt-stub/src/test/kotlin/io/grpc/kt/EchoServiceImpl.kt) for full example.)
```kotlin
class EchoServiceImpl : EchoGrpcKt.EchoImplBase() {
  override suspend fun unary(req: EchoProto.EchoReq): EchoProto.EchoResp {
    return EchoProto.EchoResp.newBuilder()
      .setId(req.id)
      .setValue(req.value)
      .build()
  }

  ...

  override suspend fun bidiStreaming(req: ReceiveChannel<EchoProto.EchoReq>): ReceiveChannel<EchoProto.EchoResp> {
    return GlobalScope.produce {
      for (reqMsg in req) {
        val respMsg = EchoProto.EchoResp.newBuilder()
          .setId(reqMsg.id)
          .setValue(reqMsg.value)
          .build()
        send(respMsg)
      }
    }
  }
}
```

* Client side
```kotlin
val stub = EchoGrpcKt.newStub(channel)

// unary
runBlocking {
  val req = EchoProto.EchoReq.newBuilder()
    .setId(1)
    .setValue("Hello")
    .build()
  val resp = stub.unary(req)
}

// bidirectional streaming
runBlocking {
  val req: ReceivingChannel<EchoProto.EchoReq> = ...
  val resp = stub.bidiStreaming(req)
  for(msg in resp) {
    // do something with the msg
  }
}
```