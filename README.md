# GRPC-Kotlin
![Travis](https://travis-ci.org/xiaodongw/grpc-kt.svg?branch=master)

This is a GRPC stub & compiler for Kotlin Coroutine.

This library is still under development.

## Getting started


## Publish
Publish to Maven Central

* Publish to Sonatype
```
gradle clean :grpc-kt-compiler:uploadArchives -PtargetOs=linux -PtargetArch=x86_64
gradle clean :grpc-kt-compiler:uploadArchives -PtargetOs=windows -PtargetArch=x86_64
gradle clean :grpc-kt-compiler:uploadArchives -PtargetOs=osx -PtargetArch=x86_64
gradle :grpc-kt-stub:uploadArchives
```

* Promote to Maven Central
  * Go to https://oss.sonatype.org/
  * Close the staging repository if there is no problem.
  * Release the repository if close succeeded.