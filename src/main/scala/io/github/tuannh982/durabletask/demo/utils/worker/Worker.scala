package io.github.tuannh982.durabletask.demo.utils.worker

import io.github.tuannh982.durabletask.demo.utils.logging.SimpleLogging

import scala.concurrent.{ExecutionContext, Future}

trait Worker extends SimpleLogging {

  @volatile
  private var running = true

  def loop(): Unit

  def start()(implicit ec: ExecutionContext): Future[Unit] = {
    Future {
      logger.info("worker started")
      try {
        while (running) {
          loop()
        }
      } catch {
        case e: Exception =>
          logger.error(e)
          throw e
        case e: Throwable =>
          logger.fatal(e)
          throw e
      }
    }
  }

  def stop(): Unit = {
    running.synchronized {
      running = false
    }
  }
}
