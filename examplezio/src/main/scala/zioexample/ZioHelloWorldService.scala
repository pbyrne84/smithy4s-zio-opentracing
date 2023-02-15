package zioexample

import org.http4s.Request
import smithy4s.hello.{Greeting, HelloWorldService, PersonResponse, PersonUpdatePayload}
import zio.logging.backend.SLF4J
import zio.telemetry.opentelemetry.Tracing
import zio.{Task, UIO, ZIO, ZLayer}
import zioexample.service.ZioExampleService
import zioexample.tracing.TracedRequest

// We need the request as we want the headers and the url for setting up the trace/MDC json logging.
// It is nice to have certain things such a url info as an annotation in the logging.
// The headers are required by the open tracing.
// We should not require on the request for any business logic as that indicates a potential break in the
// smithy4s contract.
class ZioHelloWorldService(eventualRequest: UIO[Request[Task]])
    extends HelloWorldService[Task]
    with TracedRequest {

  // Guarantees java call will follow structure
  // https://zio.github.io/zio-logging/docs/overview/overview_slf4j
  //
  // https://github.com/pbyrne84/zio2playground/blob/main/src/main/scala/com/github/pbyrne84/zio2playground/logging/ZIOHack.scala
  // Will add it to mdc before java calls
  private val loggingLayer: ZLayer[Any, Nothing, Unit] = SLF4J.slf4j

  def hello(
      name: String,
      town: Option[String]
  ): Task[Greeting] = {
    for {
      request <- eventualRequest
      greeting <- ZioExampleService
        .hello(request, name, town)
        // As Task is expected as the return type in the signature this blocks any injection past this point
        // e.g Task[Greeting] versus ZIO[Any with Tracing, Nothing, Greeting]
        // The furthest left generic (Any with Tracing) is for the dependencies required
        // which are satiated by the provide.
        .provide(zio.telemetry.opentelemetry.Tracing.live, TracingLayers.tracerLayer, loggingLayer)
    } yield greeting
  }

  override def updatePerson(data: PersonUpdatePayload): Task[PersonResponse] = {
    for {
      request <- eventualRequest
      result <- traced(data.name, request) {
        for {
          /*
            // Logs should show tracing details e.g.
            {
            "@timestamp": "2023-02-17T10:55:52.836Z",
            "@version": "1",
            "message": "Starting to update person PersonUpdatePayload(person-name,None,None)",
            "logger_name": "zioexample.ZioHelloWorldService",
            "thread_name": "ZScheduler-9",
            "level": "INFO",
            "level_value": 20000,
            "span_name": "POST:/person",
            "parent_span_id": "81aeb025909473a7",
            "incoming_request": "POST-/person",
            "trace": "zioexample.tracing.B3Tracing.startTracing(B3Tracing.scala:21)",
            "trace_id": "fb4c7c9cf16393bf2e01c8d5f31898a9",
            "span_id": "e205ae206e864e42"
          }
           */
          _ <- ZIO.log(s"Starting to update person $data")
          tracedResponse <- Tracing.getCurrentSpan.map(currentSpan =>
            PersonResponse(
              data = data,
              traceId = currentSpan.getSpanContext.getTraceId,
              spanId = currentSpan.getSpanContext.getSpanId,
              sampled = "1"
            )
          )
        } yield tracedResponse
      }
    } yield result

  }.provide(zio.telemetry.opentelemetry.Tracing.live, TracingLayers.tracerLayer, loggingLayer)

}
