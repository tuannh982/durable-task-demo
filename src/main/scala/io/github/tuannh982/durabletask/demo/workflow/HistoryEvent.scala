package io.github.tuannh982.durabletask.demo.workflow

sealed trait HistoryEvent

object HistoryEvent {

  case class ActivityScheduled(
    taskID: Int,
    activityClass: String,
    encodedInput: String
  ) extends HistoryEvent

  case class ActivityCompleted(
    taskID: Int,
    encodedOutput: String
  ) extends HistoryEvent

  case class ActivityFailed(
    taskID: Int,
    encodedError: String
  ) extends HistoryEvent

  case class WorkflowCompleted(
    instanceID: String,
    encodedOutput: String
  ) extends HistoryEvent

  case class WorkflowFailed(
    instanceID: String,
    encodedError: String
  ) extends HistoryEvent
}
