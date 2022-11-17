package zioexample.tracing

import io.opentelemetry.api.trace.{SpanId, SpanKind, StatusCode, TraceId}
import io.opentelemetry.context.propagation.TextMapPropagator
import io.opentelemetry.extension.trace.propagation.B3Propagator
import zhttp.http.Request
import zio.telemetry.opentelemetry.Tracing
import zio.{Task, Trace, ZIO}
import zioexample.logging.ExampleLogAnnotations

//Copied from https://github.com/pbyrne84/zio2playground/blob/main/src/main/scala/com/github/pbyrne84/zio2playground/tracing/B3Tracing.scala
object B3Tracing {

  val b3Propagator: TextMapPropagator = B3Propagator.injectingMultiHeaders()
  val headerTextMapGetter: ZioHeaderTextMapGetter = new ZioHeaderTextMapGetter()

  def requestInitialisationSpan[A, B, C](name: String, request: org.http4s.Request[Task])(
      operation: ZIO[A, B, C]
  )(implicit trace: Trace): ZIO[A with Tracing, B, C] = {
    import zio.telemetry.opentelemetry.TracingSyntax.OpenTelemetryZioOps

    // The generics of this much match headerTextMapGetter
    // Header is an alias of String->String in it
    val headersAsTuple: List[(String, String)] =
      request.headers.headers.map(header => header.name.toString -> header.value)

    operation
      .spanFrom(
        propagator = b3Propagator,
        carrier = headersAsTuple,
        getter = headerTextMapGetter,
        spanName = name,
        spanKind = SpanKind.SERVER
      ) @@ ExampleLogAnnotations.incomingRequest(request)
  }

  // Creates a new span and makes sure all the new details gets set in the logging context.
  // Very confusing just using Tracing.span otherwise.
  // Span names should be fairly non unique as we can use them to search in kibana etc.
  // So you can use traceId and spanName to locate things. Though you can also make things a bit
  // more flexible like client-call-success or client-call-fail as then it is quite easy
  // to look at those things over a period to spot any errant patterns.
  // There are fun games of spot the difference and less than fun ones.
  //
  // A less fun game of spot the difference  is to strip nulls in all payloads
  // then the spec has to be continuously referenced on any problems.
  // I put that in the evil category of how to make someones day bad.
  //
  // Implicit trace will just mark where the serverSpan was called unless it has been passed down
  // e.g  "trace": "com.github.pbyrne84.zio2playground.http.TracingHttp.initialiseB3Trace.applyOrElse(TracingHttp.scala:41)"
  def serverSpan[R, E, A](
      name: String
  )(effect: => ZIO[R, E, A])(implicit trace: Trace): ZIO[R with Tracing, E, A] = {
    Tracing.span[R with Tracing, E, A](name, SpanKind.SERVER, defaultErrorMapper) {

      import TracingOps._

      for {
        span <- Tracing.getCurrentSpan
        spanContext = span.getSpanContext
        traceId = spanContext.getTraceId
        spanId = spanContext.getSpanId
        parentSpanId = span.maybeParentSpanId.getOrElse("") // this will not exist unless sampled
        result <- effect @@ ExampleLogAnnotations.stringTraceId(traceId) @@
          ExampleLogAnnotations.parentSpanId(parentSpanId) @@
          ExampleLogAnnotations.stringSpanId(spanId) @@
          ExampleLogAnnotations.stringSpanName(name) @@
          ExampleLogAnnotations.trace(trace.toString)

      } yield result
    }
  }

  // Not sure what really should be used at this point
  private def defaultErrorMapper[E]: PartialFunction[E, StatusCode] = { case a =>
    StatusCode.ERROR
  }
}
