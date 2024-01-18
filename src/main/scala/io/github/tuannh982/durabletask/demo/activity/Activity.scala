package io.github.tuannh982.durabletask.demo.activity

import scala.reflect.ClassTag

abstract class Activity[I, O](
  implicit val inputCt: ClassTag[I],
  implicit val outputCt: ClassTag[O]
) {
  final val inputClass: Class[_]  = inputCt.runtimeClass
  final val outputClass: Class[_] = outputCt.runtimeClass

  def execute(input: I): O

  def executeInternal(ctx: ActivityExecutionContext): Unit = {
    try {
      val input  = ctx.getInput(inputClass)
      val output = execute(inputCt.unapply(input).get)
      ctx.complete(output)
    } catch {
      case e: Exception => ctx.fail(e)
    }
  }
}
