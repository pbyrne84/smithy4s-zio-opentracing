import cats.data._
import cats.effect.{IO, IOLocal}
import cats.syntax.all._
import org.http4s.headers._
import org.http4s.{Header, HttpRoutes, Request}
import trace._
import zio.Task

object ZIOMiddleware {

  import zio.interop.catz._

  implicit class RequestOps(request: Request[Task]) {
    def maybeHeader[A, B](map: A => B)(implicit
        ev: Header[A, Header.Single]
    ): Option[B] =
      request.headers.get[A].map { a: A => map(a) }
  }

  def withRequestInfo(
      routes: HttpRoutes[Task]
  ): HttpRoutes[Task] =
    HttpRoutes[Task] { request: Request[Task] =>
      val requestInfo = extractTracingHeaders(request)

      routes(request)
    }

  private def extractTracingHeaders(request: Request[Task]): RequestInfo = {
    RequestInfo(
      maybeContentType = request.maybeHeader[`Content-Type`, String](contentType =>
        s"${contentType.mediaType.mainType}/${contentType.mediaType.subType}"
      ),
      maybeUserAgent = request.maybeHeader[`User-Agent`, String](_.product.toString),
      maybeB3TraceId = request.maybeHeader[`X-B3-TraceId`, B3TraceId](header => B3TraceId(header)),
      maybeB3SpanId = request.maybeHeader[`X-B3-SpanId`, B3SpanId](header => B3SpanId(header)),
      maybeB3ParentSpanId = request
        .maybeHeader[`X-B3-ParentSpanId`, B3ParentSpanId](header => B3ParentSpanId(header)),
      maybeB3Sampled = request.maybeHeader[`X-B3-Sampled`, B3Sampled](header => B3Sampled(header))
    )
  }
}
