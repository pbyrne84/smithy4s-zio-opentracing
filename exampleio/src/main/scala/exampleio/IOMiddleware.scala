package exampleio

import cats.data._
import cats.effect.{IO, IOLocal}
import cats.syntax.all._
import org.http4s.headers._
import org.http4s.{Header, HttpRoutes, Request}
import trace._

object IOMiddleware {
  private implicit class RequestOps(request: Request[IO]) {
    def maybeHeader[A, B](map: A => B)(implicit
        ev: Header[A, Header.Single]
    ): Option[B] =
      request.headers.get[A].map { a: A => map(a) }
  }

  def withRequestInfo(
      routes: HttpRoutes[IO],
      ioLocalRequestInfo: IOLocal[RequestInfo]
  ): HttpRoutes[IO] =
    HttpRoutes[IO] { request: Request[IO] =>
      val requestInfo = extractTracingHeaders(request)

      val updatedLocalRequestInfo: OptionT[IO, Unit] =
        OptionT.liftF(ioLocalRequestInfo.set(requestInfo))

      updatedLocalRequestInfo *> routes(request)
    }

  def extractTracingHeaders(request: Request[IO]): RequestInfo = {
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
