package io.github.tuannh982.durabletask.demo.utils.codec

trait Codec {
  def encode(v: Any): String
  def decode[T](v: String, clazz: Class[T]): T
  def tryDecode(v: String): Any
}
