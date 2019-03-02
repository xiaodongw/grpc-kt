plugins {
  `kotlin-dsl`      // use Gradle Kotlin DSL for build files
}

repositories {
  jcenter()
  maven("https://plugins.gradle.org/m2/")
}

dependencies {
  compile("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.21")
  compile("com.google.gradle:osdetector-gradle-plugin:1.6.2")
  compile("com.google.protobuf:protobuf-gradle-plugin:0.8.8")
}