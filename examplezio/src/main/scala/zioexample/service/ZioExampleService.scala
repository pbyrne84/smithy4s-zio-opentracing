package zioexample.service

import io.opentelemetry.api.trace.Span
import org.http4s.Request
import smithy4s.hello.Greeting
import zio.{Task, ZIO}
import zio.telemetry.opentelemetry.Tracing
import zioexample.tracing.{B3Tracing, TracedRequest}

object ZioExampleService {

  def hello(
      request: Request[Task],
      name: String,
      maybeTown: Option[String],
      alwaysTrace: Boolean = true
  ): ZIO[Any with Tracing, Nothing, Greeting] =
    new ZioExampleService().hello(request, name, maybeTown, alwaysTrace)
}

class ZioExampleService extends TracedRequest {
  def hello(
      request: Request[Task],
      name: String,
      maybeTown: Option[String],
      alwaysTrace: Boolean = true
  ): ZIO[Any with Tracing, Nothing, Greeting] = {
    // Grabs request from above and runs it all through io.opentelemetry via zio. This sets the fibre up with current
    // trace information. traced is in t zioexample.tracing.TracedRequest
    // Note unless both a valid trace id amd span id are in the headers a new trace will be generated
    traced("hello-request", request, alwaysTrace) {
      for {
        greeting <- childSpan1(name, maybeTown)
      } yield greeting
    }
  }

  private def childSpan1(name: String, town: Option[String]) = {
    // this will initialise logging for the span with all the trace ids etc
    // across all child operations until a new span is created as a child
    //
    // Note "X-B3-Sampled 1" header controls parent linking
    // Logging will create something like
    // {
    //  "@timestamp": "2022-11-11T15:09:05.636Z",
    //  "@version": "1",
    //  "message": "chupacabra",
    //  "logger_name": "zioexample.ZioCatsHelloWorldService",
    //  "thread_name": "ZScheduler-Worker-1",
    //  "level": "INFO",
    //  "level_value": 20000,
    //  "span_name": "hello-route",
    //  "parent_span_id": "6b4e0af4721e53a8",
    //  "incoming_request": "GET-/A",
    //  "trace": "zioexample.ZioCatsHelloWorldService.hello(ZioCatsHelloWorldService.scala:50)",
    //  "trace_id": "7026a11ee2cab43f877b35a616f2c781",
    //  "span_id": "fd1a0fd55af28b28"
    // }
    B3Tracing
      .serverSpan("hello-route") {
        for {
          _ <- ZIO.log("chupacabra")
          currentSpan <- Tracing.getCurrentSpan
          greeting = town
            .map(t => createGreeting(s"Hello $name from $t!", currentSpan))
            .getOrElse(createGreeting(s"Hello $name!", currentSpan))

          _ <- childSpan2

        } yield {
          greeting
        }
      }
  }

  private def createGreeting(name: String, currentSpan: Span): Greeting =
    Greeting(
      message = s"Hello $name!",
      traceId = currentSpan.getSpanContext.getTraceId,
      parentSpanId = currentSpan.getSpanContext.getTraceId,
      spanId = currentSpan.getSpanContext.getSpanId,
      sampled = "1"
    )

  private def childSpan2 = {
    B3Tracing
      .serverSpan("hello-route-child-span") {
        ZIO.log("demogorgon")
      }
  }

}
