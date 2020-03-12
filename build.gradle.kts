import build.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`  // Enables Kotlin DSL for Gradle
  `java-library`
  kotlin("jvm") apply false  // Enables Kotlin Gradle plugin
  signing
  `maven-publish`
  id("com.github.ben-manes.versions").version("0.28.0")
}

allprojects {
  group = "com.github.xiaodongw"
  version = "0.4.0"

  apply {
    plugin("java")
    plugin<KotlinPlatformJvmPlugin>()
    plugin("maven-publish")
    plugin("com.google.protobuf")
    plugin("signing")
  }

  repositories {
    mavenCentral()
    maven("https://dl.bintray.com/kotlin/kotlinx")
  }

  dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Deps.junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Deps.junitVersion}")
    testRuntimeOnly("org.slf4j:slf4j-simple:1.7.25")
  }

  sourceSets {
    main {
      java {
        srcDir("$projectDir/gen/main/java")
      }
    }

    test {
      java {
        srcDir("$projectDir/gen/test/java")
      }
    }
  }

  tasks {
    named("clean").configure {
      doFirst {
        delete("gen")
      }
    }

    withType<Test> {
      useJUnitPlatform {
        includeEngines("junit-jupiter")
      }
    }

    withType<KotlinCompile> {
      kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xuse-experimental=kotlin.Experimental")
      }
    }

    withType<Test> {
      testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
        events("passed", "skipped", "failed")
      }
    }
  }

  publishing {
    repositories {
      maven {
        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        val ossrhUsername: String? by project
        val ossrhPassword: String? by project
        credentials {
          username = ossrhUsername
          password = ossrhPassword
        }
      }
    }
  }

  publishing
  signing {
    isRequired = false
  }
}
