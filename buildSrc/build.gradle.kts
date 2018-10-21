buildscript {
  // this is not required once kotlin 1.3 released
  repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
  }
}

plugins {
  `kotlin-dsl`
  id("com.google.osdetector").version("1.4.0")
}

repositories {
  jcenter()
  maven("https://plugins.gradle.org/m2/")
  maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
  // must use same kotlin version as gradle
  //compile("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.61")
  // must use same kotlin version as gradle
  // compile("org.jetbrains.kotlin:kotlin-gradle-plugin:1.2.61")

  //compile("com.google.protobuf:protobuf-gradle-plugin:0.8.6")
}