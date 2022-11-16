# smithy4s-zio-opentracing

A repo with an example of **smithy4s** (https://github.com/disneystreaming/smithy4s) and ZIO 2 with tracing/logging and testing.\
This implementation is based off https://disneystreaming.github.io/smithy4s/docs/overview/quickstart/.

smithy4s generates final tagless service which expects an implementation that fits  ```IO[_]``` or ```Task[_]```. This in turn blocks
the dependency management for ZIO as Task is **ZIO[Any, Throwable, A]**, the furthest left generic is where ZIO puts the layer 
requirements. For example a call that requires tracing would be **ZIO[Any with Tracing, Nothing, Greeting]**. This
means that we likely have to use the implementation that fits Task[_] to not hold business logic but simply handle the 
dependency management. Unless you can control your dependencies when testing it can make testing very hard, anything 
involving time and dates tends to be very fragile and can lead to a culture of "rerun and it will work" which should not
be promoted in an ideal world, tests that periodically fail can be symptomatic of deeper issues.

More details of tracing can be found at
[https://github.com/pbyrne84/zio2playground](https://github.com/pbyrne84/zio2playground)


## Implementation parts

### Blaze server

Trying to find an ember reference proved hard.

```scala
package zioexample

import org.http4s.Request
import org.http4s.blaze.server.BlazeServerBuilder
import zio.{FiberRef, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

object ZIOMain extends ZIOAppDefault {

  private val emptyRequest: Request[Task] = Request[Task]()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    initialiseBlaze

  }

  def initialiseBlaze: ZIO[Scope, Throwable, Unit] = {
    // I always put implicit imports close to use as it makes things easier to understand and also easy to copy.
    // If they are used throughout the class then I would put them under the class definition so it is not
    // lost in the main imports. Implicits can be a large barrier to entry.
    // Intellij found **zio.interop.catz.implicits.rts** for me.
    import zio.interop.catz._
    import zio.interop.catz.implicits.rts

    FiberRef.make(emptyRequest).flatMap { emptyRequestFibreRef: FiberRef[Request[Task]] =>
      ZIO.executor
        .flatMap { executor =>
          ZIORoutes.getAll(emptyRequestFibreRef).toScopedZIO.flatMap { routes =>
            BlazeServerBuilder[Task]
              .withExecutionContext(executor.asExecutionContext)
              .bindHttp(8080, "localhost")
              .withHttpWebSocketApp(_ => routes.orNotFound)
              .serve
              .compile
              .drain
          }
        }
        .provide(Scope.default)
    }
  }
}
```

### Middleware

An implementation based on https://disneystreaming.github.io/smithy4s/docs/guides/extract-request-info/#middleware. 
Where that one splits out things out and hides the request this implementation just passes the request as we use it 
to set up the tracing later. As mentioned it should not be used for contract based things directly. In the later logging example
it there is a **"incoming_request": "GET-/A",** value in the json logging.

```scala
package zioexample

import cats.data.OptionT
import org.http4s.{HttpRoutes, Request}
import zio.{FiberRef, Task}

object ZIOMiddleware {

  def withRequestInfo(
      routes: HttpRoutes[Task],
      requestFibreRef: FiberRef[Request[Task]]
  ): HttpRoutes[Task] = {
    import zio.interop.catz.asyncInstance

    HttpRoutes[Task] { request: Request[Task] =>
      val requestOnFibreRefForRequest: OptionT[Task, Unit] =
        OptionT.liftF(requestFibreRef.set(request))

      import cats.implicits.catsSyntaxApply
      requestOnFibreRefForRequest *> routes(request)
    }
  }
}
```

### Routes 

Used by **ZIOMain**. This will bind everything together creating swagger endpoints as well.

```scala
package zioexample

import cats.effect.kernel.Resource
import org.http4s.{HttpRoutes, Request}
import zio.{FiberRef, Task, UIO}

object ZIORoutes {

  import zio.interop.catz._
  import zio.interop.catz.implicits.rts

  private val docs =
    smithy4s.http4s.swagger.docs[Task](smithy4s.hello.HelloWorldService)

  def getAll(
      emptyRequestFibreRef: FiberRef[Request[Task]]
  ): Resource[Task, HttpRoutes[Task]] = {

    smithy4s.http4s.SimpleRestJsonBuilder
      .routes(new ZioHelloWorldService(createEventualRequest(emptyRequestFibreRef)))
      .resource
      .map { routes =>
        import cats.implicits.toSemigroupKOps
        ZIOMiddleware.withRequestInfo(routes <+> docs, emptyRequestFibreRef)
      }
  }

  private def createEventualRequest(
      ioLocalHeaders: FiberRef[Request[Task]]
  ): UIO[Request[Task]] = {
    ioLocalHeaders.get
  }
}

```

### ZioHelloWorldService
This extends the generated **HelloWorldService[_]**.
At this point we lose ability to customise injection. This can act as a facade to any service calls that then
require their dependencies to be setup.

```scala
package zioexample

import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.http4s.Request
import smithy4s.hello.{Greeting, HelloWorldService}
import zio.logging.backend.SLF4J
import zio.{Task, UIO, ZIO, ZLayer}
import zioexample.service.ZioExampleService

// We need the request as we want the headers and the url for setting up the trace/MDC json logging.
// It is nice to have certain things such a url info as an annotation in the logging.
// The headers are required by the open tracing.
// We should not require on the request for any business logic as that indicates a potential break in the
// smithy4s contract.
class ZioHelloWorldService(eventualRequest: UIO[Request[Task]]) extends HelloWorldService[Task] {

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

```


### ZIO Example Service

This example starts the tracing using the request. It uses the **io.opentelemetry.extension.trace.propagation.B3Propagator.injectingMultiHeaders()**
open telemetry B3 tracing underneath and in this example we default to sampled unless the sample header is explicitly 
set to **X-B3-Sampled: 0**, I like to hoard and hoarding is useful when it comes to working out failure.

This can be tested eaily as we can control the dependencies going in.

The spec ZioExampleServiceSpec tests that the trace id is added to log messages and that the parent->child span 
relations exist in the logs.

```scala
package zioexample.service

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
      defaultToAlwaysSample: Boolean = true
  ): ZIO[Any with Tracing, Nothing, Greeting] =
    new ZioExampleService().hello(request, name, maybeTown, defaultToAlwaysSample)
}

class ZioExampleService extends TracedRequest {
  def hello(
      request: Request[Task],
      name: String,
      maybeTown: Option[String],
      defaultToAlwaysSample: Boolean = true
  ): ZIO[Any with Tracing, Nothing, Greeting] = {
    // Grabs request from above and runs it all through io.opentelemetry via zio. This sets the fibre up with current
    // trace information. traced is in t zioexample.tracing.TracedRequest
    // Note unless both a valid trace id amd span id are in the headers a new trace will be generated
    traced("hello-request", request, defaultToAlwaysSample) {
      for {
        greeting <- childSpan1(name, maybeTown)
      } yield greeting
    }
  }

  private def childSpan1(name: String, town: Option[String]) = {
    // this will initialise logging for the span with all the trace ids etc
    // across all child operations until a new span is created as a child
    //
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
          greetingMessage = town
            .map(t => "Hello $name from $t!")
            .getOrElse(s"Hello $name!")

          _ <- childSpan2
          greeting <- createTracedGreeting(greetingMessage)

        } yield greeting
      }
  }

  private def childSpan2 = {
    B3Tracing
      .serverSpan("hello-route-child-span") {
        ZIO.log("demogorgon")
      }
  }

  private def createTracedGreeting(name: String): ZIO[Tracing, Nothing, Greeting] = {
    // We create a span at the end so the OutgoingRequestTracing
    // https://github.com/pbyrne84/smithy4s-zio-opentracing/blob/c8149815cf68422aeca7421854aff8e5ac091d4b/src/main/smithy/ExampleService.smithy#L19-L18
    // gives a span/parentSpan relationship that is valid as it is the final span
    // Probably a better way to do this as this can get boiler plate heavy. Greeting is autogenerated so creating
    // Greeting.apply(message)(traceId) cannot be put there for an addTrace[A](traceable : Trace => A) : Task[A]
    // (String, String, String, String) => Greeting is pretty horrid.
    // method. Could be generated at some point.
    B3Tracing
      .serverSpan("response") {
        Tracing.getCurrentSpan.map(currentSpan =>
          Greeting(
            message = s"Hello $name!",
            traceId = currentSpan.getSpanContext.getTraceId,
            spanId = currentSpan.getSpanContext.getSpanId,
            sampled = "1"
          )
        )
      }

  }

}

```




#### Other project overviews
[https://pbyrne84.github.io/](https://pbyrne84.github.io/)


