package io.grpc.kt.core

object LogUtils {
  fun <T> objectString(obj: T): String {
    require(obj is Object)
    return obj.javaClass.name + "@" + Integer.toHexString(obj.hashCode())
  }
}
