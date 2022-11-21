package exampleio.trace

import cats.effect.{IO, IOLocal}

import java.util

object LocalTrace {
  def start = {
    IOLocal("")

  }
}

class LocalTrace(private val traceMap: Map[String, String] = Map.empty[String, String]) {

  def update(key: String, value: String): LocalTrace = {
    new LocalTrace(traceMap.updated(key, value))
  }

  def get(key: String): Option[String] =
    traceMap.get(key)

  def startTrace( ) = {

  }
}
