import com.google.protobuf.gradle.*
import groovy.lang.GString
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.*
import build.*

plugins {
  id("com.google.protobuf")
}

description = "gRPC: Kotlin"

dependencies {
  compile("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.0.0-RC1")
  compile("io.grpc:grpc-netty:${Constants.grpcJavaVersion}")
  compile("io.grpc:grpc-protobuf:${Constants.grpcJavaVersion}")
  compile("io.grpc:grpc-stub:${Constants.grpcJavaVersion}")
  compile("org.slf4j:slf4j-api:1.7.25")

  testImplementation("org.junit.jupiter:junit-jupiter-api:${Constants.junitVersion}")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Constants.junitVersion}")
  testCompile("org.slf4j:slf4j-simple:1.7.25")
}

val genDir = "$projectDir/gen"
configure<ProtobufConvention> {
  protobuf(closureOf<ProtobufConfigurator> {
    generatedFilesBaseDir = genDir

    protoc(delegateClosureOf<ExecutableLocator> {
      artifact = "com.google.protobuf:protoc:${Constants.protocVersion}"
    })

    plugins(delegateClosureOf<NamedDomainObjectContainer<ExecutableLocator>> {
      this {
        register("grpc") {
          path = pluginPath()
        }
      }
    })

    generateProtoTasks(delegateClosureOf<ProtobufConfigurator.GenerateProtoTaskCollection> {
      all().forEach { task ->
        task.plugins(delegateClosureOf<NamedDomainObjectContainer<GenerateProtoTask.PluginOptions>> {
          this {
            register("grpc")
          }
        })
      }
    })
  })
}

configure<JavaPluginConvention> {
  sourceSets(closureOf< SourceSetContainer> {
    getByName("test").java.srcDir("${genDir}/test/java")
    getByName("test").java.srcDir("${genDir}/test/grpc")
  })
}

tasks.named("compileTestKotlin").configure {
  dependsOn("generateTestProto")
}

tasks.withType<Test> {
  useJUnitPlatform {
    includeEngines("junit-jupiter")
  }

  testLogging {
    events("passed", "skipped", "failed")
    exceptionFormat = TestExceptionFormat.FULL
  }
}
