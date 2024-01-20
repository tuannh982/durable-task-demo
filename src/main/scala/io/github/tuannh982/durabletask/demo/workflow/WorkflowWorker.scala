package io.github.tuannh982.durabletask.demo.workflow

import io.github.tuannh982.durabletask.demo.activity.Activity
import io.github.tuannh982.durabletask.demo.backend.Backend
import io.github.tuannh982.durabletask.demo.task.WorkflowTask
import io.github.tuannh982.durabletask.demo.utils.codec.Codec
import io.github.tuannh982.durabletask.demo.utils.logging.SimpleLogging
import io.github.tuannh982.durabletask.demo.utils.worker.QueueWorker

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.reflect.ClassTag
import scala.util.Try

private[workflow] class WorkflowExecutionContextImpl(
  codec: Codec,
  task: WorkflowTask,
  history: List[HistoryEvent],
  lastSuspendedOffset: Int
) extends WorkflowExecutionContext
    with SimpleLogging {
  outer =>

  /**
    * workflow local task
    */
  private class LocalScheduledTask[T](
    taskId: Int,
    promise: Promise[T],
    val outputCt: ClassTag[T]
  ) extends ScheduledTask[T] {

    def complete(t: Try[Any]): Unit = {
      promise.complete(t.map { v =>
        outputCt.unapply(v) match {
          case Some(value) => value
          case None        => throw NondeterministicException(s"task completed with wrong type, result $v")
        }
      })
    }

    override def get(): T = {
      while (!promise.isCompleted && historyPlayer.handleNextEvent()) {}
      if (promise.isCompleted) {
        Await.result(promise.future, Duration.Inf) // it's completed anyway
      } else {
        // we've processed all events, suspend workflow
        throw WorkflowBlocked(s"wait for task #$taskId to complete")
      }
    }
  }

  /**
    * history player
    */
  private class HistoryPlayer {
    private var historyTapeCursor: Int = 0
    var isReplaying: Boolean           = 0 <= lastSuspendedOffset

    def handleNextEvent(): Boolean = {
      if (historyTapeCursor > lastSuspendedOffset) {
        isReplaying = false
      }
      if (historyTapeCursor < history.length) {
        val event = history(historyTapeCursor)
        outer.onEvent(event)
        historyTapeCursor += 1
      }
      historyTapeCursor < history.length
    }
  }

  /**
    * workflow context
    */

  private class LocalWorkflowContext extends WorkflowContext {

    override def isReplaying: Boolean = historyPlayer.isReplaying

    override def scheduleActivity[I, O](activity: Activity[I, O], input: I): ScheduledTask[O] = {
      val encodedInput = codec.encode(input)
      val taskID       = sequenceNo.getAndIncrement()
      val action       = WorkflowAction.ScheduleActivity(instanceID, taskID, activity.getClass.getName, encodedInput)
      if (pendingActions.contains(taskID)) {
        throw NondeterministicException(s"task $taskID existed")
      }
      val promise = Promise[O]()
      val task    = new LocalScheduledTask[O](taskID, promise, activity.outputCt)
      pendingActions.put(taskID, action)
      openTasks.put(taskID, task)
      task
    }
  }

  private def onEvent(event: HistoryEvent): Unit = {
    event match {
      case HistoryEvent.ActivityScheduled(taskID, _, _) =>
        Option(pendingActions.remove(taskID)) match {
          case Some(_) => (): Unit
          case None    => throw NondeterministicException(s"scheduled task $taskID not found")
        }
      case HistoryEvent.ActivityCompleted(taskID, encodedOutput) =>
        Option(openTasks.remove(taskID)) match {
          case Some(task) => task.complete(util.Success(codec.decode(encodedOutput, task.outputCt.runtimeClass)))
          case None       => (): Unit // maybe duplicated event, ignoring
        }
      case HistoryEvent.ActivityFailed(taskID, encodedError) =>
        Option(openTasks.remove(taskID)) match {
          case Some(task) =>
            val decodedError = codec.tryDecode(encodedError).asInstanceOf[Exception]
            task.complete(util.Failure(decodedError))
          case None => (): Unit // maybe duplicated event, ignoring
        }
      case HistoryEvent.WorkflowCompleted(_, _) => (): Unit // ignore this event
      case HistoryEvent.WorkflowFailed(_, _)    => (): Unit // ignore this event
      case HistoryEvent.WorkflowSuspended(_)    => (): Unit // ignore this event
    }
  }

  // format: off
  private val instanceID: String = task.instanceID
  private val workflowClass: Class[Workflow[_, _]] = Class.forName(task.workflowClass).asInstanceOf[Class[Workflow[_, _]]]
  private val historyPlayer                                            = new HistoryPlayer()
  private val workflowContext                                          = new LocalWorkflowContext()
  private val sequenceNo: AtomicInteger                                = new AtomicInteger(0)
  private val pendingActions: ConcurrentHashMap[Int, WorkflowAction]   = new ConcurrentHashMap[Int, WorkflowAction]()
  private val openTasks: ConcurrentHashMap[Int, LocalScheduledTask[_]] = new ConcurrentHashMap[Int, LocalScheduledTask[_]]()
  private var executionResult: Option[WorkflowExecutionResult]         = None
  // format: on

  /**
    * execution context methods
    */
  override def getInput[T](clazz: Class[T]): T = codec.decode(task.encodedInput, clazz)

  override def complete[T](result: T): Unit = {
    executionResult = Some(WorkflowExecutionResult.Completed(codec.encode(result)))
  }

  override def fail[E <: Exception](error: E): Unit = {
    logger.error(error)
    executionResult = Some(WorkflowExecutionResult.Failed(codec.encode(error)))
  }

  override def suspend(): Unit = {
    executionResult = Some(WorkflowExecutionResult.Suspended)
  }

  override def execute(): (Boolean, List[HistoryEvent], List[WorkflowAction]) = {
    val instance = workflowClass.getDeclaredConstructor().newInstance()
    instance.executeInternal(this, workflowContext)
    // try process all remaining history events
    while (historyPlayer.handleNextEvent()) {}
    //
    val baseActions = pendingActions.values().asScala.toList
    var finished    = true
    val extraActions = executionResult match {
      case Some(result) =>
        result match {
          case WorkflowExecutionResult.Completed(encodedResult) =>
            List(WorkflowAction.CompleteWorkflow(instanceID, encodedResult))
          case WorkflowExecutionResult.Failed(encodedError) =>
            List(WorkflowAction.FailWorkflow(instanceID, encodedError))
          case WorkflowExecutionResult.Suspended =>
            finished = false
            List(WorkflowAction.SuspendWorkflow(instanceID))
        }
      case None => throw new RuntimeException("execution result must be set")
    }
    val actions = baseActions ++ extraActions
    val events  = WorkflowExecutionContextImpl.actionsToHistoryEvents(actions)
    (finished, events, actions)
  }
}

private[workflow] object WorkflowExecutionContextImpl {

  private def actionsToHistoryEvents(actions: List[WorkflowAction]): List[HistoryEvent] = {
    actions.map {
      case WorkflowAction.CompleteWorkflow(instanceID, encodedOutput) =>
        HistoryEvent.WorkflowCompleted(instanceID, encodedOutput)
      case WorkflowAction.FailWorkflow(instanceID, encodedError) =>
        HistoryEvent.WorkflowFailed(instanceID, encodedError)
      case WorkflowAction.SuspendWorkflow(instanceID) =>
        HistoryEvent.WorkflowSuspended(instanceID)
      case WorkflowAction.ScheduleActivity(_, taskID, activityClass, encodedInput) =>
        HistoryEvent.ActivityScheduled(taskID, activityClass, encodedInput)
    }
  }
}

class WorkflowWorker(codec: Codec, backend: Backend) extends QueueWorker[WorkflowTask] {
  override protected def poll(): Option[WorkflowTask] = backend.pollWorkflowTask()

  override def handle(task: WorkflowTask): Unit = {
    logger.debug(s"task polled: $task")
    val history             = backend.fetchHistory(task.instanceID)
    val lastSuspendedOffset = backend.getWorkflowMetadata(task.instanceID).get.lastSuspendedOffset
    val ctx                 = new WorkflowExecutionContextImpl(codec, task, history, lastSuspendedOffset)
    logger.debug(s"start workflow execution ${task.instanceID}")
    val (finished, events, actions) = ctx.execute()
    logger.debug(s"workflow execution ${task.instanceID} finished, new events $events")
    backend.appendHistory(task.instanceID, events)
    // schedule activity tasks if needed
    for (action <- actions) {
      action match {
        case WorkflowAction.ScheduleActivity(instanceID, taskID, activityClass, encodedInput) =>
          backend.scheduleActivityTask(instanceID, taskID, activityClass, encodedInput)
        case _ => // ignore
      }
    }
    // schedule workflow task if needed
    if (!finished) {
      backend.scheduleWorkflowTask(task.instanceID, task.workflowClass, task.encodedInput)
    }
  }
}
