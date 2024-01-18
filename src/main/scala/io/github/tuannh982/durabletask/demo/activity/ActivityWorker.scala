package io.github.tuannh982.durabletask.demo.activity

import io.github.tuannh982.durabletask.demo.backend.Backend
import io.github.tuannh982.durabletask.demo.task.ActivityTask
import io.github.tuannh982.durabletask.demo.utils.codec.Codec
import io.github.tuannh982.durabletask.demo.utils.logging.SimpleLogging
import io.github.tuannh982.durabletask.demo.utils.worker.QueueWorker
import io.github.tuannh982.durabletask.demo.workflow.HistoryEvent

private[activity] class ActivityExecutionContextImpl(
  codec: Codec,
  task: ActivityTask
) extends ActivityExecutionContext
    with SimpleLogging {
  // format: off
  private var executionResult: Option[ActivityExecutionResult] = None
  private val activityClass: Class[Activity[_, _]] = Class.forName(task.activityClass).asInstanceOf[Class[Activity[_, _]]]
  // format: on

  override def getInput[T](clazz: Class[T]): T = {
    codec.decode(task.encodedInput, clazz)
  }

  override def complete[T](result: T): Unit = {
    executionResult = Some(ActivityExecutionResult.Completed(codec.encode(result)))
  }

  override def fail[E <: Exception](error: E): Unit = {
    logger.error(error)
    executionResult = Some(ActivityExecutionResult.Failed(codec.encode(error)))
  }

  override def execute(): ActivityExecutionResult = {
    val instance = activityClass.getDeclaredConstructor().newInstance()
    instance.executeInternal(this)
    executionResult match {
      case Some(result) => result
      case None         => throw new RuntimeException("execution result must be set")
    }
  }
}

class ActivityWorker(codec: Codec, backend: Backend) extends QueueWorker[ActivityTask] {
  override protected def poll(): Option[ActivityTask] = backend.pollActivityTask()

  override def handle(task: ActivityTask): Unit = {
    logger.debug(s"task polled: $task")
    val ctx    = new ActivityExecutionContextImpl(codec, task)
    val result = ctx.execute()
    val event = result match {
      case ActivityExecutionResult.Completed(encodedResult) =>
        HistoryEvent.ActivityCompleted(task.taskID, encodedResult)
      case ActivityExecutionResult.Failed(encodedError) =>
        HistoryEvent.ActivityFailed(task.taskID, encodedError)
    }
    backend.appendHistory(task.instanceID, List(event))
  }
}
