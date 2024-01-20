package io.github.tuannh982.durabletask.demo.workflow

import io.github.tuannh982.durabletask.demo.activity.Activity

trait ScheduledTask[T] {
  def get(): T
}

trait WorkflowContext {
  def isReplaying: Boolean
  def scheduleActivity[I, O](activity: Activity[I, O], input: I): ScheduledTask[O]
}
