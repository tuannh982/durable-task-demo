package io.github.tuannh982.durabletask.demo.task

case class ActivityTask(
  instanceID: String,
  taskID: Int,
  activityClass: String,
  encodedInput: String
)

case class WorkflowTask(
  instanceID: String,
  workflowClass: String,
  encodedInput: String
)
