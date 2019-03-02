import build.*

description = "The protoc plugin for gRPC Kotlin Coroutine"

pluginSupport("protoc-gen-grpc-kt")

dependencies {
  testImplementation(project(":grpc-kt-stub"))
}