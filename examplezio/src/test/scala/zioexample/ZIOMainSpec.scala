package zioexample

import fs2.Stream
import io.circe.{Decoder, Encoder, ParsingFailure}
import org.http4s.{Header, Headers, Method, Request, Response, Status, Uri}
import org.typelevel.ci.CIString
import smithy4s.hello.PersonUpdatePayload
import zio.test._
import zio.{FiberRef, Scope, Task}
import zioexample.tracing.B3

object ZIOMainSpec extends ZIOSpecDefault {

  import io.circe.generic.semiauto._
  import io.circe.syntax._
  import org.http4s.implicits._
  import zio.interop.catz._
  import zio.interop.catz.implicits.rts

  private implicit val personUpdatePayloadDecoder: Decoder[PersonUpdatePayload] =
    deriveDecoder[PersonUpdatePayload]

  private implicit val personUpdatePayloadEncoder: Encoder[PersonUpdatePayload] =
    deriveEncoder[PersonUpdatePayload]

  private val traceId = "5551c45e2603e06d6f1ddad7dfbd5772"
  private val spanId = "125ba5de516b2d0b"

  object TraceHeaders {
    def fromResponse(response: Response[Task]): TraceHeaders =
      TraceHeaders(
        response.headers.headers.map(header => header.name.toString -> header.value)
      )
  }

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

  object ErrorResponse {
    import io.circe.generic.semiauto._

    implicit val errorResponseDecoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
  }
  case class ErrorResponse(message: String, uri: String)

  implicit class BytesOps(bytes: scala.Vector[Byte]) {
    import io.circe.parser.decode
    private val rawString = new String(bytes.toArray)

    def bytesAsErrorResponse: Either[ParsingFailure, ErrorResponse] = {

      decode[ErrorResponse](rawString).left
        .map(error => ParsingFailure(s"Could not parse ${rawString}", error))
    }

    def bytesAsErrorResponseUri: Either[ParsingFailure, String] =
      bytesAsErrorResponse.map(_.uri)

    def bytesAsPersonUpdatePayload: Either[ParsingFailure, PersonUpdatePayload] = {
      decode[PersonUpdatePayload](rawString).left
        .map(error => ParsingFailure(s"Could not parse $rawString", error))

    }
  }

  // Usually we would separate the error handler out into its own test as trying to wack it at this level
  // creates a lot of duplication when testing for trace id generation. As this test actually does no
  // business logic underneath the lack of clarity this duplication can cause is less of an issue.
  override def spec: Spec[Any with Scope, Throwable] =
    suite(getClass.getSimpleName)(
      suite("get person should")(
        test(
          "add the trace headers to the response for a found person when a trace is not passed in"
        ) {
          for {
            routes <- getRoutes
            validRequest = Request[Task](uri = Uri.unsafeFromString("/person/MOOO"))
            response <- routes.orNotFound(validRequest)
            entityBody = response.body
            bodyBytes <- entityBody.compile.toVector
            traceHeaders = TraceHeaders.fromResponse(response)
          } yield assertTrue(
            response.status == Status.Ok,
            new String(bodyBytes.toArray) == "{\"message\":\"Hello Hello MOOO!!\"}",
            traceHeaders.traceExistenceMap == Map(
              "traceId" -> true,
              "spanId" -> true,
              "sampled" -> true
            )
          )
        },
        test(
          "add the trace headers to the response for a found person when a trace is passed in"
        ) {
          for {
            routes <- getRoutes
            validRequest = Request[Task](
              uri = Uri.unsafeFromString("/person/MOOO"),
              headers = rawTraceHeaders(sampled = "1")
            )
            response <- routes.orNotFound(validRequest)
            entityBody = response.body
            bodyBytes <- entityBody.compile.toVector
            traceHeaders = TraceHeaders.fromResponse(response)
          } yield assertTrue(
            response.status == Status.Ok,
            new String(bodyBytes.toArray) == "{\"message\":\"Hello Hello MOOO!!\"}",
            traceHeaders.maybeTraceId.contains(traceId), // same trace id should come out
            !traceHeaders.maybeSpanId.contains(spanId), // new spanId should be generated
            traceHeaders.traceExistenceMap == Map(
              "traceId" -> true,
              "spanId" -> true,
              "sampled" -> true
            )
          )
        },
        test(
          "add trace headers using passed trace id when the name in the url does not match a required format"
        ) {
          for {
            routes <- getRoutes
            url = "/person/999999"
            validRequest = Request[Task](
              uri = Uri.unsafeFromString(url),
              headers = rawTraceHeaders(sampled = "1")
            )
            response <- routes.orNotFound(validRequest)
            entityBody = response.body
            bodyBytes <- entityBody.compile.toVector
            traceHeaders = TraceHeaders.fromResponse(response)
          } yield assertTrue(
            response.status == Status.BadRequest,
            bodyBytes.bytesAsErrorResponseUri == Right(url),
            traceHeaders.maybeTraceId.contains(traceId), // same trace id should come out
            !traceHeaders.maybeSpanId.contains(spanId), // new spanId should be generated
            traceHeaders.traceExistenceMap == Map(
              "traceId" -> true,
              "spanId" -> true,
              "sampled" -> true
            )
          )
        },
        test(
          "add trace headers generating trace id when not passed when the name in the url does not match a required format"
        ) {
          for {
            routes <- getRoutes
            url = "/person/999999"
            validRequest = Request[Task](
              uri = Uri.unsafeFromString(url)
            )
            response <- routes.orNotFound(validRequest)
            entityBody = response.body
            bodyBytes <- entityBody.compile.toVector
            traceHeaders = TraceHeaders.fromResponse(response)
          } yield assertTrue(
            response.status == Status.BadRequest,
            bodyBytes.bytesAsErrorResponseUri == Right(url),
            !traceHeaders.maybeTraceId.contains(traceId), // new trace id generated
            !traceHeaders.maybeSpanId.contains(spanId), // new spanId should be generated
            traceHeaders.traceExistenceMap == Map(
              "traceId" -> true,
              "spanId" -> true,
              "sampled" -> true
            )
          )
        }
      ),
      suite("update person should")(
        test(
          "fail when the require name field is missing and adding a new trace in the response as none was passed"
        ) {
          for {
            routes <- getRoutes
            url = "/person"
            validRequest = Request[Task](
              uri = Uri.unsafeFromString(url),
              method = Method.POST,
              body = Stream.emits("{}".getBytes("UTF-8"))
            )
            response <- routes.orNotFound(validRequest)
            traceHeaders = TraceHeaders.fromResponse(response)
          } yield assertTrue(
            response.status == Status.BadRequest,
            !traceHeaders.maybeTraceId.contains(traceId), // new trace id generated
            !traceHeaders.maybeSpanId.contains(spanId), // new spanId should be generated
            traceHeaders.traceExistenceMap == Map(
              "traceId" -> true,
              "spanId" -> true,
              "sampled" -> true
            )
          )
        },
        test(
          "fail when the require name field is missing and uses the same trace passed in"
        ) {
          for {
            routes <- getRoutes
            url = "/person"
            validRequest = Request[Task](
              uri = Uri.unsafeFromString(url),
              method = Method.POST,
              body = Stream.emits("{}".getBytes("UTF-8")),
              headers = rawTraceHeaders(sampled = "1")
            )
            response <- routes.orNotFound(validRequest)
            traceHeaders = TraceHeaders.fromResponse(response)
          } yield assertTrue(
            response.status == Status.BadRequest,
            traceHeaders.maybeTraceId.contains(traceId), // same trace id used
            !traceHeaders.maybeSpanId.contains(spanId), // new spanId should be generated
            traceHeaders.traceExistenceMap == Map(
              "traceId" -> true,
              "spanId" -> true,
              "sampled" -> true
            )
          )
        },
        test(
          "succeed when the payload is correct and use the trace id id that is passed in"
        ) {
          for {
            routes <- getRoutes
            url = "/person"
            validPersonUpdate = PersonUpdatePayload("person-name")
            validRequest = Request[Task](
              uri = Uri.unsafeFromString(url),
              method = Method.POST,
              body = Stream.emits(validPersonUpdate.asJson.spaces2.getBytes("UTF-8"))
            )
            response <- routes.orNotFound(validRequest)
            bodyBytes <- response.body.compile.toVector
            traceHeaders = TraceHeaders.fromResponse(response)
          } yield assertTrue(
            response.status == Status.Ok,
            bodyBytes.bytesAsPersonUpdatePayload == Right(validPersonUpdate),
            !traceHeaders.maybeTraceId.contains(traceId), // same trace id used
            !traceHeaders.maybeSpanId.contains(spanId), // new spanId should be generated
            traceHeaders.traceExistenceMap == Map(
              "traceId" -> true,
              "spanId" -> true,
              "sampled" -> true
            )
          )
        },
        test(
          "succeed when the payload is correct and use the trace id id that is passed in"
        ) {
          for {
            routes <- getRoutes
            url = "/person"
            validPersonUpdate = PersonUpdatePayload("person-name")
            validRequest = Request[Task](
              uri = Uri.unsafeFromString(url),
              method = Method.POST,
              body = Stream.emits(validPersonUpdate.asJson.spaces2.getBytes("UTF-8")),
              headers = rawTraceHeaders(sampled = "1")
            )
            response <- routes.orNotFound(validRequest)
            bodyBytes <- response.body.compile.toVector
            traceHeaders = TraceHeaders.fromResponse(response)
          } yield assertTrue(
            response.status == Status.Ok,
            bodyBytes.bytesAsPersonUpdatePayload == Right(validPersonUpdate),
            traceHeaders.maybeTraceId.contains(traceId), // same trace id used
            !traceHeaders.maybeSpanId.contains(spanId), // new spanId should be generated
            traceHeaders.traceExistenceMap == Map(
              "traceId" -> true,
              "spanId" -> true,
              "sampled" -> true
            )
          )
        }
      )
    )

  private def getRoutes =
    FiberRef.make(Request[Task]()).flatMap(fiberRef => ZIOMain.getRoutes(fiberRef))

  private def rawTraceHeaders(sampled: String) = Headers(
    Header.Raw(CIString(B3.header.traceId), traceId),
    Header.Raw(CIString(B3.header.spanId), spanId),
    Header.Raw(CIString(B3.header.sampled), sampled)
  )

}
