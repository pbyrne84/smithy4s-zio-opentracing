package zioexample

import org.http4s.{Header, Headers, Request, Response, Status, Uri}
import org.typelevel.ci.CIString
import zio.test._
import zio.{FiberRef, Scope, Task, ZIO}
import zioexample.tracing.B3

object ZIOMainSpec extends ZIOSpecDefault {

  import org.http4s.implicits._
  import zio.interop.catz._
  import zio.interop.catz.implicits.rts

  private val traceId = "5551c45e2603e06d6f1ddad7dfbd5772"
  private val spanId = "125ba5de516b2d0b"

  case class TraceHeaders(headers: List[(String, String)]) {

    val maybeTraceId: Option[String] = getHeaderValue(B3.header.traceId)
    val maybeSpanId: Option[String] = getHeaderValue(B3.header.spanId)
    val maybeSampled: Option[String] = getHeaderValue(B3.header.sampled)

    private def getHeaderValue(headerName: String) =
      headers.find(header => header._1 == headerName).map(_._2)

    val traceExistenceMap: Map[String, Boolean] = Map(
      "traceId" -> maybeTraceId.isDefined,
      "spanId" -> maybeSpanId.isDefined,
      "sampled" -> maybeSampled.isDefined
    )

  }

  override def spec: Spec[Any with Scope, Throwable] =
    suite("ZIOMain")(
      test("route should reply not found when when is not found") {
        for {
          fiberRef <- createEmptyRequestFibreRef
          routes <- ZIOMain.getRoutes(fiberRef)
          emptyRequest = Request[Task]()
          response <- routes.orNotFound(emptyRequest)
        } yield assertTrue(
          response.status == Status.NotFound,
          convertResponseHeaders(response) == List(
            "Content-Type" -> "text/plain; charset=UTF-8",
            "Content-Length" -> "9"
          )
        )
      },
      test("add trace headers to response on a found route when a trace is not passed in") {
        for {
          fiberRef <- createEmptyRequestFibreRef
          routes <- ZIOMain.getRoutes(fiberRef)
          validRequest = Request[Task](uri = Uri.unsafeFromString("/MOOO"))
          response <- routes.orNotFound(validRequest)
          entityBody = response.body
          bodyBytes <- entityBody.compile.toVector
          headers = convertResponseHeaders(response)
          traceHeaders = TraceHeaders(headers)
        } yield assertTrue(
          response.status == Status.Ok,
          new String(bodyBytes.toArray) == "{\"message\":\"Hello Hello MOOO!!\"}",
          extractContentHeaders(headers) == createContentHeaders("application/json", 32),
          traceHeaders.traceExistenceMap == Map(
            "traceId" -> true,
            "spanId" -> true,
            "sampled" -> true
          )
        )
      },
      test(
        "add trace headers to response on a found route a trace is passed in"
      ) {
        for {
          fiberRef <- createEmptyRequestFibreRef
          routes <- ZIOMain.getRoutes(fiberRef)
          validRequest = Request[Task](
            uri = Uri.unsafeFromString("/MOOO"),
            headers = traceHeaders(sampled = "1")
          )
          response <- routes.orNotFound(validRequest)
          entityBody = response.body
          bodyBytes <- entityBody.compile.toVector
          headers = convertResponseHeaders(response)
          traceHeaders = TraceHeaders(headers)
        } yield assertTrue(
          response.status == Status.Ok,
          new String(bodyBytes.toArray) == "{\"message\":\"Hello Hello MOOO!!\"}",
          extractContentHeaders(headers) == createContentHeaders("application/json", 32),
          traceHeaders.maybeTraceId.contains(traceId), // same trace id should come out
          !traceHeaders.maybeSpanId.contains(spanId), // new spanId should be generated
          traceHeaders.traceExistenceMap == Map(
            "traceId" -> true,
            "spanId" -> true,
            "sampled" -> true
          )
        )
      }
    )

  private def createEmptyRequestFibreRef: ZIO[Scope, Nothing, FiberRef[Request[Task]]] = {
    FiberRef.make(Request[Task]())
  }

  private def convertResponseHeaders(response: Response[Task]) = {
    response.headers.headers.map(header => header.name.toString -> header.value)
  }

  private def createContentHeaders(contentType: String, size: Int) = {
    List(
      "Content-Length" -> s"$size",
      "Content-Type" -> s"$contentType"
    )
  }

  private def extractContentHeaders(headers: List[(String, String)]) = {
    val validHeaderNames = List("Content-Length", "Content-Type")
    headers.filter(header => validHeaderNames.contains(header._1)).sorted
  }

  private def traceHeaders(sampled: String) = Headers(
    Header.Raw(CIString(B3.header.traceId), traceId),
    Header.Raw(CIString(B3.header.spanId), spanId),
    Header.Raw(CIString(B3.header.sampled), sampled)
  )

}
