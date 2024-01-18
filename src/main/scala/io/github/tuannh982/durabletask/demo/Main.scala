package io.github.tuannh982.durabletask.demo

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.github.tuannh982.durabletask.demo.activity.{Activity, ActivityWorker}
import io.github.tuannh982.durabletask.demo.backend.{MockBackend, MockClient}
import io.github.tuannh982.durabletask.demo.utils.codec.CirceCodec
import io.github.tuannh982.durabletask.demo.utils.logging.{LogLevel, SimpleLogging}
import io.github.tuannh982.durabletask.demo.workflow.{Workflow, WorkflowContext, WorkflowWorker}

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object Main extends SimpleLogging {

  case class DemoWorkflowInput(a: Int, b: Int, c: Int)
  case class IntTuple2(a: Int, b: Int)

  class GreetingActivity extends Activity[String, Unit] {

    override def execute(input: String): Unit = {
      println(s"hello $input")
    }
  }

  class SumActivity extends Activity[IntTuple2, Int] {

    override def execute(input: IntTuple2): Int = {
      input.a + input.b
    }
  }

  class MultiplyActivity extends Activity[IntTuple2, Int] {

    override def execute(input: IntTuple2): Int = {
      input.a * input.b
    }
  }

  class DemoWorkflow extends Workflow[DemoWorkflowInput, Int] {
    private val greetingActivity = new GreetingActivity
    private val sumActivity      = new SumActivity
    private val multiplyActivity = new MultiplyActivity

    override def execute(ctx: WorkflowContext, input: DemoWorkflowInput): Int = {
      ctx.scheduleActivity(greetingActivity, "workflow").get()
      val sumAB         = ctx.scheduleActivity(sumActivity, IntTuple2(input.a, input.b)).get()
      val sumABThenMulC = ctx.scheduleActivity(multiplyActivity, IntTuple2(sumAB, input.c)).get()
      val result        = sumABThenMulC
      result
    }
  }

  private val codec = new CirceCodec(
    Map(
      classOf[DemoWorkflowInput].getName -> deriveEncoder[DemoWorkflowInput],
      classOf[IntTuple2].getName         -> deriveEncoder[IntTuple2]
    ),
    Map(
      classOf[DemoWorkflowInput].getName -> deriveDecoder[DemoWorkflowInput],
      classOf[IntTuple2].getName         -> deriveDecoder[IntTuple2]
    )
  )

  private val backend        = new MockBackend()
  private val client         = new MockClient(codec, backend)
  private val workflowWorker = new WorkflowWorker(codec, backend)
  private val activityWorker = new ActivityWorker(codec, backend)

  private val workerThreadPool = Executors.newCachedThreadPool()
  private val workerEc         = ExecutionContext.fromExecutor(workerThreadPool)

  {
    backend.logger.logLevel = LogLevel.Debug
    client.logger.logLevel = LogLevel.Debug
    workflowWorker.logger.logLevel = LogLevel.Debug
    activityWorker.logger.logLevel = LogLevel.Debug
  }

  def main(args: Array[String]): Unit = {
    // init workers
    workflowWorker.start()(workerEc)
    activityWorker.start()(workerEc)
    // test our workflow
    val instanceID = "test-workflow-id"
    client.scheduleWorkflow(instanceID, classOf[DemoWorkflow], DemoWorkflowInput(1, 2, 3))
    // sleep for sometime, wait for workflow to complete
    Thread.sleep(2000)
    val result = client.getWorkflowResult(instanceID)
    logger.info(s"workflow result: $result")
    // cleanup
    workflowWorker.stop()
    activityWorker.stop()
    workerThreadPool.shutdown()
  }
}
