package zioexample

import cats.effect.kernel.Resource
import io.opentelemetry.api.trace.Span
import org.http4s.{HttpRoutes, Request}
import smithy4s.hello.GenericBadRequestError
import smithy4s.http.HttpContractError
import zio.logging.backend.SLF4J
import zio.telemetry.opentelemetry.Tracing
import zio.{Cause, FiberRef, Task, UIO, ZIO}
import zioexample.logging.ExampleLogAnnotations
import zioexample.tracing.TracedRequest

object ZIORoutes extends TracedRequest {

  import zio.interop.catz._
  import zio.interop.catz.implicits.rts

  private val docs =
    smithy4s.http4s.swagger.docs[Task](smithy4s.hello.HelloWorldService)

  /** @param emptyRequestFibreRef
    *   \- we can in theory get the current request from this as we need that for tracing etc.
    * @return
    *   routes with traced BadRequest error handling and swagger endpoint
    */
  def getRoutes(
      emptyRequestFibreRef: FiberRef[Request[Task]]
  ): Resource[Task, HttpRoutes[Task]] = {

    smithy4s.http4s.SimpleRestJsonBuilder
      .routes(new ZioHelloWorldService(createEventualRequest(emptyRequestFibreRef)))
      .flatMapErrors { case e: HttpContractError =>
        for {
          request <- emptyRequestFibreRef.get
          _ = request.uri
          (traceId, spanId) <- extractTraceInformation(e, request)
        } yield GenericBadRequestError(
          traceId = traceId,
          spanId = spanId,
          sampled = "1",
          message = e.getMessage,
          uri = request.uri.toString
        )
      }
      .resource
      .map { routes =>
        import cats.implicits.toSemigroupKOps

        val value = routes <+> docs
        ZIOMiddleware.withRequestInfo(value, emptyRequestFibreRef)
      }
  }

  private def createEventualRequest(
      ioLocalHeaders: FiberRef[Request[Task]]
  ): UIO[Request[Task]] = {
    ioLocalHeaders.get
  }

  private def extractTraceInformation(e: Throwable, request: Request[Task]) = {
    // traced initialise
    traced("error-request", request) {
      for {
        // for public apis need to be careful with extracting bodies, though need to take into
        // account without the body any errors maybe very hard to repeat. Errors that
        // are hard to repeat get kicked into the long grass and can be symptomatic of much
        // bigger issues
        bodyBytes <- request.body.compile.toList
        bodyString = new String(bodyBytes.toArray)
        _ <- ZIO.logErrorCause(
          s"Failed executing request ${request.withEmptyBody} with $bodyString",
          Cause.fail(e)
        ) @@ ExampleLogAnnotations.incomingRequestBody(bodyString)
        context <- Tracing.getCurrentContext
        spanContext = Span.fromContext(context).getSpanContext
        traceId = spanContext.getTraceId
        spanId = spanContext.getSpanId

      } yield (traceId, spanId)

    }.provide(zio.telemetry.opentelemetry.Tracing.live, TracingLayers.tracerLayer, SLF4J.slf4j)

  }
}
