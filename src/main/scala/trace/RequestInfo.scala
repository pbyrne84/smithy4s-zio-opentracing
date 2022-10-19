package trace

case class RequestInfo(
    maybeContentType: Option[String] = None,
    maybeUserAgent: Option[String] = None,
    maybeB3TraceId: Option[B3TraceId] = None,
    maybeB3SpanId: Option[B3SpanId] = None,
    maybeB3ParentSpanId: Option[B3ParentSpanId] = None,
    maybeB3Sampled: Option[B3Sampled] = None
) {

  val kernalHeaders: Map[String, String] = List(
    maybeB3TraceId,
    maybeB3SpanId,
    maybeB3ParentSpanId,
    maybeB3Sampled
  ).collect { case Some(b3HeaderValue: B3Value[_]) =>
    b3HeaderValue.name -> b3HeaderValue.stringValue
  }.toMap

}
