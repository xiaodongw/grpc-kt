import java.text.SimpleDateFormat
import java.util.*
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformJvmPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
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
  }
//  apply plugin: "checkstyle"
//  apply plugin: "java"
//  apply plugin: "maven"
//  apply plugin: "idea"
//  apply plugin: "signing"
//  apply plugin: "jacoco"

  group = "com.github.xiaodongw"
  version = "0.1.0"

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

//  signing {
//    required false
//    sign configurations.archives
//  }
//
//  // Disable JavaDoc doclint on Java 8. It's annoying.
//  if (JavaVersion.current().isJava8Compatible()) {
//    allprojects {
//      tasks.withType(Javadoc) {
//        options.addStringOption('Xdoclint:none', '-quiet')
//      }
//    }
//  }
//
//  checkstyle {
//    configFile = file("$rootDir/checkstyle.xml")
//    toolVersion = "6.17"
//    ignoreFailures = false
//    if (rootProject.hasProperty("checkstyle.ignoreFailures")) {
//      ignoreFailures = rootProject.properties["checkstyle.ignoreFailures"].toBoolean()
//    }
//    configProperties["rootDir"] = rootDir
//  }
//
//  checkstyleMain {
//    source = fileTree(dir: "src/main", include: "**/*.java")
//  }
//
//  checkstyleTest {
//    source = fileTree(dir: "src/test", include: "**/*.java")
//  }
//
//  task javadocJar(type: Jar) {
//    classifier = 'javadoc'
//    from javadoc
//  }
//
//  task sourcesJar(type: Jar) {
//    classifier = 'sources'
//    from sourceSets.main.allSource
//  }
//
//  artifacts {
//    archives javadocJar, sourcesJar
//  }
//
//  uploadArchives.repositories.mavenDeployer {
//    beforeDeployment { MavenDeployment deployment -> signing.signPom(deployment) }
//    String stagingUrl
//    if (rootProject.hasProperty('repositoryId')) {
//      stagingUrl = 'https://oss.sonatype.org/service/local/staging/deployByRepositoryId/' +
//          rootProject.repositoryId
//    } else {
//      stagingUrl = 'https://oss.sonatype.org/service/local/staging/deploy/maven2/'
//    }
//    def configureAuth = {
//      if (rootProject.hasProperty('ossrhUsername') && rootProject.hasProperty('ossrhPassword')) {
//        authentication(userName: rootProject.ossrhUsername, password: rootProject.ossrhPassword)
//      }
//    }
//    repository(url: stagingUrl, configureAuth)
//    snapshotRepository(url: 'https://oss.sonatype.org/content/repositories/snapshots/', configureAuth)
//
//    pom.project {
//      name "grpc-rx"
//      description 'GRPC stub & compiler for RxJava2'
//      url 'https://github.com/xiaodongw/grpc-rx'
//
//      scm {
//        url 'https://github.com/xiaodongw/grpc-rx.git'
//      }
//
//      licenses {
//        license {
//          name 'The Apache License, Version 2.0'
//          url 'http://www.apache.org/licenses/LICENSE-2.0.txt'
//        }
//      }
//
//      developers {
//        developer {
//          id 'xiaodongw'
//          name 'Xiaodong Wang'
//          email 'xiaodongw79@gmail.com'
//        }
//      }
//    }
//  }
}
