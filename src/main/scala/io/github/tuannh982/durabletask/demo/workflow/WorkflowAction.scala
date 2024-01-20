package io.github.tuannh982.durabletask.demo.workflow

sealed trait WorkflowAction

object WorkflowAction {

  case class CompleteWorkflow(
    instanceID: String,
    encodedOutput: String
  ) extends WorkflowAction

  case class FailWorkflow(
    instanceID: String,
    encodedError: String
  ) extends WorkflowAction

  case class SuspendWorkflow(
    instanceID: String
  ) extends WorkflowAction

  case class ScheduleActivity(
    instanceID: String,
    taskID: Int,
    activityClass: String,
    encodedInput: String
  ) extends WorkflowAction
}
