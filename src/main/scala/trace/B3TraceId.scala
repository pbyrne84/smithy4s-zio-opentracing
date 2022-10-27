package trace

import org.http4s.headers.`X-B3-TraceId`

import java.util

object B3TraceId {
  def apply(header: `X-B3-TraceId`): B3TraceId = {
    val str = `X-B3-TraceId`.headerInstance.value(header)
    B3TraceId(str, header)
  }
}

case class B3TraceId(stringValue: String, header: `X-B3-TraceId`)
