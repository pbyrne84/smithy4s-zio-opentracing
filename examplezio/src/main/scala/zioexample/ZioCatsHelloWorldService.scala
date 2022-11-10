package zioexample

import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.http4s.Request
import smithy4s.hello.{Greeting, HelloWorldService}
import zio.logging.backend.SLF4J
import zio.{Task, UIO, ZIO, ZLayer}
import zioexample.service.ZioExampleService

class ZioCatsHelloWorldService(val eventualRequest: UIO[Request[Task]])
    extends HelloWorldService[Task] {

  // builder builder builder to get around inaccessibility of things
  private val tracer: Tracer =
    SdkTracerProvider.builder().build().tracerBuilder(this.getClass.toString).build()

  private val tracerLayer = ZLayer(ZIO.succeed(tracer))

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
        .provide(zio.telemetry.opentelemetry.Tracing.live, tracerLayer, loggingLayer)
    } yield greeting
  }

}
