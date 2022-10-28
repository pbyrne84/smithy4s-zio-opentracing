import cats.data._
import cats.effect.{IO, IOLocal}
import org.http4s.{Header, HttpRoutes, Request}
import cats.syntax.all._
import natchez.Kernel
import org.http4s.headers.{
  `Content-Type`,
  `User-Agent`,
  `X-B3-ParentSpanId`,
  `X-B3-Sampled`,
  `X-B3-SpanId`,
  `X-B3-TraceId`
}
import trace._

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
      val requestInfo = extractTracingHeaders(request)

      val kernel = Kernel(requestInfo.kernalHeaders)

      OptionT.liftF(local.set(requestInfo)) *> routes(request)
    }

  private def extractTracingHeaders(request: Request[IO]): RequestInfo = {
    RequestInfo(
      maybeContentType = request.attemptGetHeader[`Content-Type`, String](contentType =>
        s"${contentType.mediaType.mainType}/${contentType.mediaType.subType}"
      ),
      maybeUserAgent = request.attemptGetHeader[`User-Agent`, String](_.product.toString),
      maybeB3TraceId =
        request.attemptGetHeader[`X-B3-TraceId`, B3TraceId](header => B3TraceId(header)),
      maybeB3SpanId = request.attemptGetHeader[`X-B3-SpanId`, B3SpanId](header => B3SpanId(header)),
      maybeB3ParentSpanId = request
        .attemptGetHeader[`X-B3-ParentSpanId`, B3ParentSpanId](header => B3ParentSpanId(header)),
      maybeB3Sampled =
        request.attemptGetHeader[`X-B3-Sampled`, B3Sampled](header => B3Sampled(header))
    )
  }
}
