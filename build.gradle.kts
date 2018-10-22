import java.text.SimpleDateFormat
import java.util.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.impldep.org.apache.maven.artifact.ant.RemoteRepository
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.math.sign

buildscript {
  repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
  }

  dependencies {
    classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.6")
  }
}

plugins {
  `kotlin-dsl`
  id ("org.jetbrains.kotlin.jvm").version("1.3.0-rc-116").apply(false)
  id("com.google.osdetector").version("1.4.0").apply(false)
  id("com.github.ben-manes.versions").version("0.17.0").apply(false)
}

subprojects {
  apply {
    plugin<KotlinPlatformJvmPlugin>()
    plugin("com.github.ben-manes.versions")
    plugin("signing")
    plugin("maven")
  }

  group = "com.github.xiaodongw"
  version = "0.2.0"

  repositories {
    mavenCentral()
    maven("https://kotlin.bintray.com/kotlinx")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
  }

  tasks.withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "1.8"
    }
  }

  configure<SigningExtension> {
    isRequired = false
    sign(configurations.archives.get())
  }

  tasks.register("sourcesJar", Jar::class) {
    classifier = "sources"
    from(sourceSets.main.get().allSource)
  }


  artifacts {
    val sourcesJar = tasks.named("sourcesJar")
    add("archives", sourcesJar)
  }

  tasks.named<Upload>("uploadArchives") {
    repositories {
      withConvention(MavenRepositoryHandlerConvention::class) {
        mavenDeployer {
          withGroovyBuilder {
            "beforeDeployment" {
              project.configure<SigningExtension> {
                signPom(delegate as MavenDeployment)
              }
            }
          }

          withGroovyBuilder {
            "repository"("url" to uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")) {
              "authentication"(mapOf(
                "userName" to rootProject.properties["ossrhUsername"],
                "password" to rootProject.properties["ossrhPassword"]
              ))
            }
          }
            //"snapshotRepository"("url" to uri("$buildDir/m2/snapshots"))

          pom.project {
            withGroovyBuilder {
              "name"("grpc-kt")
              "description"("GRPC stub & compiler for RxJava2")
              "url"("https://github.com/xiaodongw/grpc-kt")
              "scm" {
                "url"("https://github.com/xiaodongw/grpc-kt.git")
              }
              "licenses" {
                "license" {
                  "name"("The Apache License, Version 2.0")
                  "url"("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
              }

              "developers" {
                "developer" {
                  "id"("xiaodongw")
                  "name"("Xiaodong Wang")
                  "email"("xiaodongw79@gmail.com")
                }
              }
            }
          }
        }
      }
    }
  }
}
