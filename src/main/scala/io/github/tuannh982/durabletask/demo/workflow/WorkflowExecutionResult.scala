package io.github.tuannh982.durabletask.demo.workflow

sealed trait WorkflowExecutionResult

object WorkflowExecutionResult {
  case class Completed(encodedResult: String) extends WorkflowExecutionResult
  case class Failed(encodedError: String)     extends WorkflowExecutionResult
  object Suspended                            extends WorkflowExecutionResult
}
