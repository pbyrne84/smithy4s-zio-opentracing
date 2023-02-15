package zioexample.tracing

import org.http4s.{Headers, Request}
import zio.telemetry.opentelemetry.Tracing
import zio.{Task, ZIO}

trait TracedRequest {

  def traced[A, B, C](
      spanName: String,
      request: Request[Task],
      defaultToAlwaysSample: Boolean = true
  )(call: => ZIO[A, B, C]): ZIO[A with Tracing, B, C] = {

    for {
      defaultingSampledRequest <- ZIO.succeed {
        val currentHeaders =
          request.headers.headers.map(header => header.name.toString -> header.value)

        val headersWithDefaultingSampleHeaders =
          B3.defaultSampledHeader(currentHeaders, defaultToAlwaysSample)

        val headers = Headers(headersWithDefaultingSampleHeaders)

        request.withHeaders(headers)
      }

      tracedOperation <- B3Tracing.startTracing(spanName, defaultingSampledRequest)(
        call
      )
    } yield tracedOperation
  }
}
