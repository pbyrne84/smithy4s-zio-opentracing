package trace

import org.http4s.Header
import org.http4s.headers.{`X-B3-ParentSpanId`, `X-B3-Sampled`, `X-B3-SpanId`, `X-B3-TraceId`}

sealed abstract class B3[A, B](implicit val headerInstance: Header[A, Header.Single]) {
  protected def genericApply(stringValue: String, header: A): B

  def apply(header: A): B = {
    val str = headerInstance.value(header)
    genericApply(str, header)
  }

}

object B3TraceId extends B3[`X-B3-TraceId`, B3TraceId] {
  override protected def genericApply(stringValue: String, header: `X-B3-TraceId`): B3TraceId =
    B3TraceId(stringValue, header)


}

case class B3TraceId(stringValue: String, header: `X-B3-TraceId`)

object B3SpanId extends B3[`X-B3-SpanId`, B3SpanId] {
  override def genericApply(stringValue: String, header: `X-B3-SpanId`): B3SpanId =
    B3SpanId(stringValue, header)

}

case class B3SpanId(stringValue: String, header: `X-B3-SpanId`)

object B3ParentSpanId extends B3[`X-B3-ParentSpanId`, B3ParentSpanId] {
  override def genericApply(stringValue: String, header: `X-B3-ParentSpanId`): B3ParentSpanId =
    B3ParentSpanId(stringValue, header)

}

case class B3ParentSpanId(stringValue: String, header: `X-B3-ParentSpanId`)

object B3Sampled extends B3[`X-B3-Sampled`, B3Sampled] {
  override protected def genericApply(stringValue: String, header: `X-B3-Sampled`): B3Sampled =
    B3Sampled(stringValue, header)
}

case class B3Sampled(stringValue: String, header: `X-B3-Sampled`)
