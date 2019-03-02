import build.*
import com.google.protobuf.gradle.*

description = "Kotlin Protobuf support"

librarySupport()

dependencies {
  implementation("com.google.protobuf:protobuf-java:${Deps.protobufVersion}")
}

protobuf {
  generatedFilesBaseDir = "$projectDir/gen"
  protoc {
    artifact = "com.google.protobuf:protoc:${Deps.protobufVersion}"
  }
  plugins {
    id("proto-kt") {
      path = "$rootDir/proto-kt-compiler/build/exe/protoc-gen-proto-kt${Consts.exeSuffix}"
    }
  }
  generateProtoTasks {
    ofSourceSet("test").forEach {
      it.plugins {
        id("proto-kt")
      }
    }
  }
}

sourceSets {
  test {
    java {
      srcDir("$projectDir/gen/test/proto-kt")
    }
  }
}