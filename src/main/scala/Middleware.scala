import cats.data._
import cats.effect.{IO, IOLocal}
import org.http4s.{Header, HttpRoutes, Request}
import cats.syntax.all._
import org.http4s.headers.{
  `Content-Type`,
  `User-Agent`,
  `X-B3-ParentSpanId`,
  `X-B3-Sampled`,
  `X-B3-SpanId`,
  `X-B3-TraceId`
}
import trace.B3TraceId

object Middleware {
  implicit class RequestOps(request: Request[IO]) {
    def attemptGetHeader[A, B](map: A => B)(implicit
        ev: Header[A, Header.Single]
    ): Option[B] =
      request.headers.get[A].map { a: A => map(a) }
  }

  def withRequestInfo(
      routes: HttpRoutes[IO],
      local: IOLocal[RequestInfo]
  ): HttpRoutes[IO] =
    HttpRoutes[IO] { request: Request[IO] =>
      val requestInfo = RequestInfo(
        maybeContentType = request.attemptGetHeader[`Content-Type`, String](contentType =>
          s"${contentType.mediaType.mainType}/${contentType.mediaType.subType}"
        ),
        maybeUserAgent = request.attemptGetHeader[`User-Agent`, String](_.product.toString),
        maybeB3TraceId = request.attemptGetHeader[`X-B3-TraceId`, B3TraceId](a => B3TraceId(a)),
        maybeB3SpanId = request.attemptGetHeader[`X-B3-SpanId`, String](_.toString),
        maybeB3ParentSpanId = request.attemptGetHeader[`X-B3-ParentSpanId`, String](_.toString),
        maybeB3Sampled = request.attemptGetHeader[`X-B3-Sampled`, String](_.toString)
      )

      OptionT.liftF(local.set(requestInfo)) *> routes(request)
    }

}
