package io.github.tuannh982.durabletask.demo.utils.worker

trait QueueWorker[T] extends Worker {
  protected def poll(): Option[T]
  protected def doWait(): Unit = Thread.sleep(2000)

  def handle(msg: T): Unit

  override def loop(): Unit = {
    poll() match {
      case Some(msg) => handle(msg)
      case None      => doWait()
    }
  }
}
