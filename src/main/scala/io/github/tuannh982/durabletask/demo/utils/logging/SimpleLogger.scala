package io.github.tuannh982.durabletask.demo.utils.logging

import java.io.{PrintWriter, StringWriter}

final class SimpleLogger(className: String) {
  var logLevel: LogLevel = LogLevel.Info

  def trace(msg: String): Unit               = log(msg, LogLevel.Trace)
  def debug(msg: String): Unit               = log(msg, LogLevel.Debug)
  def info(msg: String): Unit                = log(msg, LogLevel.Info)
  def warn(msg: String): Unit                = log(msg, LogLevel.Warn)
  def error(msg: String): Unit               = log(msg, LogLevel.Error)
  def error(msg: String, e: Throwable): Unit = log(s"$msg\n${stackTrace(e)}", LogLevel.Error)
  def fatal(msg: String): Unit               = log(msg, LogLevel.Fatal)
  def fatal(msg: String, e: Throwable): Unit = log(s"$msg\n${stackTrace(e)}", LogLevel.Fatal)

  private def log(msg: String, level: LogLevel): Unit = {
    if (level.intLevel >= logLevel.intLevel) {
      println(f"${level.value}%-7s - $className: $msg")
    }
  }

  private def stackTrace(e: Throwable): String = {
    val out    = new StringWriter()
    val writer = new PrintWriter(out)
    e.printStackTrace(writer)
    writer.flush()
    out.toString
  }
}
