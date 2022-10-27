import cats.data._
import cats.effect.{IO, IOLocal}
import org.http4s.{Header, HttpRoutes, Request}
import cats.syntax.all._
import org.http4s.headers.{`Content-Type`, `User-Agent`}

object Middleware {
  implicit class RequestOps(request: Request[IO]) {
    def attemptGetHeader[A](map: A => String)(implicit
        ev: Header[A, Header.Single]
    ): Option[String] =
      request.headers.get[A].map { a: A => map(a) }
  }

  def withRequestInfo(
      routes: HttpRoutes[IO],
      local: IOLocal[Option[RequestInfo]]
  ): HttpRoutes[IO] =
    HttpRoutes[IO] { request: Request[IO] =>
      val requestInfo = for {
        contentType <- request.attemptGetHeader[`Content-Type`](contentType =>
          s"${contentType.mediaType.mainType}/${contentType.mediaType.subType}"
        )
        userAgent <- request.attemptGetHeader[`User-Agent`](_.product.toString)
      } yield RequestInfo(
        contentType,
        userAgent
      )

      OptionT.liftF(local.set(requestInfo)) *> routes(request)
    }

}
