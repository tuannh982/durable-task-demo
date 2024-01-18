package io.github.tuannh982.durabletask.demo.utils.logging

trait SimpleLogging {
  private lazy val className: String = this.getClass.getSimpleName
  protected lazy val logger          = new SimpleLogger(className)
}
