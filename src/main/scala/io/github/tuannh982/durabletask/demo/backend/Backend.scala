package io.github.tuannh982.durabletask.demo.backend

import io.github.tuannh982.durabletask.demo.task.{ActivityTask, WorkflowTask}
import io.github.tuannh982.durabletask.demo.workflow.HistoryEvent

trait Backend {

  def upsertWorkflowMetadata(
    instanceID: String,
    workflowClass: String,
    result: Option[WorkflowResult],
    forced: Boolean
  ): Unit
  def getWorkflowMetadata(instanceID: String): Option[WorkflowMetadata]
  def scheduleWorkflowTask(instanceID: String, workflowClass: String, encodedInput: String): Unit
  def scheduleActivityTask(instanceID: String, taskID: Int, activityClass: String, encodedInput: String): Unit
  def pollWorkflowTask(): Option[WorkflowTask]
  def pollActivityTask(): Option[ActivityTask]
  def appendHistory(instanceID: String, events: List[HistoryEvent]): Unit
  def fetchHistory(instanceID: String): List[HistoryEvent]
}
