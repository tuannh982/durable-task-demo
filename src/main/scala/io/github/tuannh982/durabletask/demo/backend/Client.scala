package io.github.tuannh982.durabletask.demo.backend

import io.github.tuannh982.durabletask.demo.workflow.Workflow

trait Client {
  def scheduleWorkflow[I, O, W <: Workflow[I, O]](instanceID: String, workflowClass: Class[W], input: I): Unit
  def getWorkflowResult(instanceID: String): Either[Exception, Any]
}
