package io.github.tuannh982.durabletask.demo.utils.logging

sealed abstract class LogLevel(val value: String, val intLevel: Int)

object LogLevel {
  object Trace extends LogLevel("TRACE", 1)
  object Debug extends LogLevel("DEBUG", 2)
  object Info  extends LogLevel("INFO", 3)
  object Warn  extends LogLevel("WARN", 4)
  object Error extends LogLevel("ERROR", 5)
  object Fatal extends LogLevel("FATAL", 6)
  object Off   extends LogLevel("OFF", Int.MaxValue)
}
