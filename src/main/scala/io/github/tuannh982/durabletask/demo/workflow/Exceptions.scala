package io.github.tuannh982.durabletask.demo.workflow

case class WorkflowBlocked(msg: String = "")           extends Exception(msg)
case class NondeterministicException(msg: String = "") extends RuntimeException(msg)
