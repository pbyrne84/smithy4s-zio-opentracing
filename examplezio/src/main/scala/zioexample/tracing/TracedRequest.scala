package zioexample.tracing

import org.http4s.{Headers, Request}
import zio.telemetry.opentelemetry.Tracing
import zio.{Task, UIO, ZIO}

trait TracedRequest {

  def traced[A, B, C](
      spanName: String,
      request: Request[Task],
      alwaysTrace: Boolean = true
  )(call: => ZIO[A, B, C]): ZIO[A with Tracing, B, C] = {

    for {
      defaultingSampledRequest <- ZIO.succeed {
        val currentHeaders =
          request.headers.headers.map(header => header.name.toString -> header.value)

        val headersWithDefaultingSampleHeaders =
          B3.defaultSampledHeader(currentHeaders, alwaysTrace)

        val headers = Headers(headersWithDefaultingSampleHeaders)

        request.withHeaders(headers)
      }

      tracedOperation <- B3Tracing.requestInitialisationSpan(spanName, defaultingSampledRequest)(
        call
      )
    } yield tracedOperation
  }
}
