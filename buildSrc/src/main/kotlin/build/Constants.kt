package build

import groovy.lang.GString
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

fun Project.exeSuffix(): String {
  //return if (osdetector.os == "windows") ".exe" else ""
  return ""
}

fun String.toGString(): GString {
  return GString.EMPTY + this
}

fun Project.pluginPath(): String {
  val exeSuffix = ""
  return "$rootDir/grpc-rx-compiler/build/exe/$Constants.protocPluginBaseName$exeSuffix"
}

//fun Project.osdetector(): OsDetectro {
//
//}

@Suppress("unused")
object Constants {
  const val protocPluginBaseName = "protoc-gen-grpc-kt"
  const val grpcJavaVersion = "1.15.1"
  const val protobufVersion = "3.5.1"
  const val protocVersion = "3.6.0"
  const val junitVersion = "5.3.1"
}
