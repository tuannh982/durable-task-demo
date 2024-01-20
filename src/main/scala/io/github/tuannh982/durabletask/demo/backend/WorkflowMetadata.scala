package io.github.tuannh982.durabletask.demo.backend

sealed trait WorkflowResult

object WorkflowResult {
  case class Completed(encodedOutput: String) extends WorkflowResult
  case class Failed(encodedError: String)     extends WorkflowResult
}

case class WorkflowMetadata(
  instanceID: String,
  workflowClass: String,
  lastSuspendedOffset: Int,
  result: Option[WorkflowResult]
) {
  def isFinished: Boolean = result.nonEmpty
}
