package io.github.tuannh982.durabletask.demo.activity

sealed trait ActivityExecutionResult

object ActivityExecutionResult {
  case class Completed(encodedResult: String) extends ActivityExecutionResult
  case class Failed(encodedError: String)     extends ActivityExecutionResult
}
