package io.github.tuannh982.durabletask.demo.activity

trait ActivityExecutionContext {
  def getInput[T](clazz: Class[T]): T
  def complete[T](result: T): Unit
  def fail[E <: Exception](error: E): Unit
  def execute(): ActivityExecutionResult
}
