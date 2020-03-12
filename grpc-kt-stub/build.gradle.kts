import build.*
import com.google.protobuf.gradle.*

description = "gRPC with Kotlin Coroutine"

librarySupport()

dependencies {
  api("io.grpc:grpc-netty:${Deps.grpcJavaVersion}")
  api("io.grpc:grpc-protobuf:${Deps.grpcJavaVersion}")
  api("io.grpc:grpc-stub:${Deps.grpcJavaVersion}")
}

protobuf {
  generatedFilesBaseDir = "$projectDir/gen"
  protoc {
    artifact = "com.google.protobuf:protoc:${Deps.protobufVersion}"
  }
  plugins {
    id("grpc-kt") {
      path = "$rootDir/grpc-kt-compiler/build/exe/protoc-gen-grpc-kt${Consts.exeSuffix}"
    }
    // generate java version for performance comparison
    id("grpc-java") {
      artifact = "io.grpc:protoc-gen-grpc-java:${Deps.grpcJavaVersion}"
    }
  }
  generateProtoTasks {
    ofSourceSet("test").forEach {
      it.plugins {
        id("grpc-kt")
        id("grpc-java")
      }
    }
  }
}

sourceSets {
  test {
    java {
      srcDir("$projectDir/gen/test/grpc-kt")
      srcDir("$projectDir/gen/test/grpc-java")
    }
  }
}