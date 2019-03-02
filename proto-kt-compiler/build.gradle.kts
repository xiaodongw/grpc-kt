import build.*

description = "The protoc plugin for Kotlin"

pluginSupport("protoc-gen-proto-kt")

dependencies {
  testImplementation(project(":proto-kt-core"))
}