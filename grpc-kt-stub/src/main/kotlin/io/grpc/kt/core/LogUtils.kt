package io.grpc.kt.core

object LogUtils {
  // fun objectString(obj: Any): String
  // this signature causes 'Type mismatch: inferred type is REQ but Object was expected' compile error, huh?

  fun <T> objectString(obj: T): String {
    require(obj is Object)
    return obj.javaClass.name + "@" + Integer.toHexString(obj.hashCode())
  }
}
