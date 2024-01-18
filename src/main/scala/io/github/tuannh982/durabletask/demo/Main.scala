package io.github.tuannh982.durabletask.demo

import io.github.tuannh982.durabletask.demo.utils.logging.SimpleLogging

object Main extends SimpleLogging {
  def main(args: Array[String]): Unit = {
    logger.error("hello", new Exception("test-exception"))
    // TODO
  }
}
