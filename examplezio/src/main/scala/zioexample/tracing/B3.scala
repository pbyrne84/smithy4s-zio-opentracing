package zioexample.tracing

import io.opentelemetry.api.trace.{SpanId, TraceId}

object B3 {

  // current context returns this as filler if the tracing is not working properly
  val emptyTraceId: String = "0".padTo(TraceId.getLength, "0").mkString
  val emptySpanId: String = "0".padTo(SpanId.getLength, "0").mkString

  object header {
    val traceId: String = "X-B3-TraceId"
    val spanId: String = "X-B3-SpanId"
    val sampled: String = "X-B3-Sampled"
  }

  /** @param headers
    * @param defaultToAlwaysSample
    *
    * If the sampled header does no exist then things like parent span ids are not set etc. Whether
    * this matter is important in logging is something else. Ideally whether the sample is worth
    * keeping is a post (tail) action versus at the start of an action. Every entrance and exit
    * point in a network distributed call chain is a major weak spot so there should be no trust.
    *
    * Only at the end of a process can whether there has been an error can be determined. Things
    * like what payload caused a deserialization fail is usually lost unless the engineer actually
    * made the effort to capture it. For example akka will use a stream that cannot be reread unless
    * converted to a strict entity. Possibly premature optimization for the amount of hassle it can
    * cause on a human level as it will require more complicated attempts at re-enactment.
    *
    * This is interesting reading
    * https://netflixtechblog.com/building-netflixs-distributed-tracing-infrastructure-bb856c319304
    */
  def defaultSampledHeader(
      headers: List[(String, String)],
      defaultToAlwaysSample: Boolean = true
  ): List[(String, String)] = {
    def generateDefaultHeader: (String, String) = {
      (
        B3.header.sampled,
        if (defaultToAlwaysSample) {
          "1"
        } else {
          "0"
        }
      )
    }

    val lowercaseSampledHeaderName = B3.header.sampled.toLowerCase
    val maybeSampledHeader =
      headers.find(header => {
        header._1.toLowerCase == lowercaseSampledHeaderName
      })

    maybeSampledHeader match {
      case Some(header) =>
        if (List("0", "1").contains(header._2)) {
          headers
        } else {
          val headersWithoutSampled = headers.filterNot { header =>
            header._1.toLowerCase == lowercaseSampledHeaderName
          }

          headersWithoutSampled :+ generateDefaultHeader
        }

      case None => headers :+ generateDefaultHeader
    }
  }

}
