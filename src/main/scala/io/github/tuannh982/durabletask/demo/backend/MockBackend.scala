package io.github.tuannh982.durabletask.demo.backend

import io.github.tuannh982.durabletask.demo.task.{ActivityTask, WorkflowTask}
import io.github.tuannh982.durabletask.demo.utils.logging.SimpleLogging
import io.github.tuannh982.durabletask.demo.workflow.HistoryEvent

import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue, TimeUnit}
import scala.collection.mutable.ListBuffer

class MockBackend extends Backend with SimpleLogging {
  private val workflowMetadata  = new ConcurrentHashMap[String, WorkflowMetadata]()
  private val history           = new ConcurrentHashMap[String, ListBuffer[HistoryEvent]]()
  private val workflowTaskQueue = new LinkedBlockingQueue[WorkflowTask]()
  private val activityTaskQueue = new LinkedBlockingQueue[ActivityTask]()

  override def upsertWorkflowMetadata(
    instanceID: String,
    workflowClass: String,
    result: Option[WorkflowResult],
    forced: Boolean
  ): Unit = {
    logger.debug(s"upsertWorkflowMetadata($instanceID, $workflowClass, $result, $forced)")
    if (forced) {
      workflowMetadata.put(instanceID, WorkflowMetadata(instanceID, workflowClass, result))
    } else {
      workflowMetadata.putIfAbsent(instanceID, WorkflowMetadata(instanceID, workflowClass, result))
    }
  }

  override def getWorkflowMetadata(instanceID: String): Option[WorkflowMetadata] = {
    val ret = Option(workflowMetadata.get(instanceID))
    logger.debug(s"getWorkflowMetadata($instanceID): $ret")
    ret
  }

  override def scheduleWorkflowTask(instanceID: String, workflowClass: String, encodedInput: String): Unit = {
    logger.debug(s"scheduleWorkflowTask($instanceID, $workflowClass, $encodedInput)")
    upsertWorkflowMetadata(instanceID, workflowClass, None, forced = false)
    val task = WorkflowTask(instanceID, workflowClass, encodedInput)
    workflowTaskQueue.put(task)
  }

  override def scheduleActivityTask(
    instanceID: String,
    taskID: Int,
    activityClass: String,
    encodedInput: String
  ): Unit = {
    logger.debug(s"scheduleWorkflowTask($instanceID, $taskID, $activityClass, $encodedInput)")
    val task = ActivityTask(instanceID, taskID, activityClass, encodedInput)
    activityTaskQueue.put(task)
  }

  override def pollWorkflowTask(): Option[WorkflowTask] = {
    val ret = Option(workflowTaskQueue.poll(1, TimeUnit.SECONDS))
    logger.debug(s"pollWorkflowTask(): $ret")
    ret
  }

  override def pollActivityTask(): Option[ActivityTask] = {
    val ret = Option(activityTaskQueue.poll(1, TimeUnit.SECONDS))
    logger.debug(s"pollActivityTask(): $ret")
    ret
  }

  override def appendHistory(instanceID: String, events: List[HistoryEvent]): Unit = {
    logger.debug(s"appendHistory($instanceID, $events)")
    if (events.nonEmpty) {
      Option(workflowMetadata.get(instanceID)) match {
        case Some(metadata) =>
          history.putIfAbsent(instanceID, new ListBuffer[HistoryEvent]())
          Option(history.get(instanceID)) match {
            case Some(historyEvents) =>
              history.put(instanceID, historyEvents ++ events)
              val updatedResult = events.last match {
                case HistoryEvent.WorkflowCompleted(_, encodedOutput) => Some(WorkflowResult.Completed(encodedOutput))
                case HistoryEvent.WorkflowFailed(_, encodedError)     => Some(WorkflowResult.Failed(encodedError))
                case _                                                => None
              }
              workflowMetadata.put(instanceID, metadata.copy(result = updatedResult))
            case None => throw new RuntimeException(s"workflow $instanceID history not found")
          }
        case None => throw new RuntimeException(s"workflow $instanceID metadata not found")
      }
    }
  }

  override def fetchHistory(instanceID: String): List[HistoryEvent] = {
    Option(workflowMetadata.get(instanceID)) match {
      case Some(_) =>
        history.putIfAbsent(instanceID, new ListBuffer[HistoryEvent]())
        Option(history.get(instanceID)) match {
          case Some(historyEvents) =>
            logger.debug(s"fetchHistory(): $historyEvents")
            historyEvents.toList
          case None => throw new RuntimeException(s"workflow $instanceID history not found")
        }
      case None => throw new RuntimeException(s"workflow $instanceID metadata not found")
    }
  }
}
