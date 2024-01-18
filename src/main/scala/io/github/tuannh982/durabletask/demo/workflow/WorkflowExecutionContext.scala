package io.github.tuannh982.durabletask.demo.workflow

trait WorkflowExecutionContext {
  def getInput[T](clazz: Class[T]): T
  def complete[T](result: T): Unit
  def fail[E <: Exception](error: E): Unit
  def suspend(): Unit
  def execute(): (Boolean, List[HistoryEvent], List[WorkflowAction])
}
