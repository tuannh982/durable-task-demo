package io.github.tuannh982.durabletask.demo.backend

import io.github.tuannh982.durabletask.demo.utils.codec.Codec
import io.github.tuannh982.durabletask.demo.utils.logging.SimpleLogging
import io.github.tuannh982.durabletask.demo.workflow.Workflow

class MockClient(codec: Codec, backend: Backend) extends Client with SimpleLogging {

  override def scheduleWorkflow[I, _, W <: Workflow[I, _]](
    instanceID: String,
    workflowClass: Class[W],
    input: I
  ): Unit = {
    backend.scheduleWorkflowTask(instanceID, workflowClass.getName, codec.encode(input))
  }

  override def getWorkflowResult(instanceID: String): Either[Exception, Any] = {
    backend.getWorkflowMetadata(instanceID) match {
      case Some(metadata) =>
        metadata.result match {
          case Some(result) =>
            result match {
              case WorkflowResult.Completed(encodedOutput) =>
                val clazz    = Class.forName(metadata.workflowClass).asInstanceOf[Class[Workflow[_, _]]]
                val instance = clazz.getDeclaredConstructor().newInstance()
                val ret      = Right(codec.decode(encodedOutput, instance.outputClass))
                logger.debug(s"getWorkflowResult($instanceID): $ret")
                ret
              case WorkflowResult.Failed(encodedError) =>
                val ret = Left(codec.tryDecode(encodedError).asInstanceOf[Exception])
                logger.debug(s"getWorkflowResult($instanceID): $ret")
                ret
            }
          case None => throw new RuntimeException(s"workflow $instanceID is not yet finished")
        }
      case None => throw new RuntimeException(s"workflow $instanceID metadata not found")
    }
  }
}
