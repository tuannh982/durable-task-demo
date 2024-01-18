package io.github.tuannh982.durabletask.demo.workflow

import scala.reflect.ClassTag

abstract class Workflow[I, O](
  implicit val inputCt: ClassTag[I],
  implicit val outputCt: ClassTag[O]
) {
  final val inputClass: Class[_]  = inputCt.runtimeClass
  final val outputClass: Class[_] = outputCt.runtimeClass
  def execute(ctx: WorkflowContext, input: I): O

  def executeInternal(ctx: WorkflowExecutionContext, c: WorkflowContext): Unit = {
    try {
      val input  = ctx.getInput(inputClass)
      val output = execute(c, inputCt.unapply(input).get)
      ctx.complete(output)
    } catch {
      case _: WorkflowBlocked           => ctx.suspend()
      case e: NondeterministicException => throw e // re-throw on non-deterministic error
      case e: Exception                 => ctx.fail(e)
    }
  }
}
