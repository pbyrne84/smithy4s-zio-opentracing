import cats.data._
import cats.effect.{IO, IOLocal}
import org.http4s.HttpRoutes
import cats.syntax.all._
import org.http4s.headers.{`Content-Type`, `User-Agent`}

object Middleware {

  def withRequestInfo(
      routes: HttpRoutes[IO],
      local: IOLocal[Option[RequestInfo]]
  ): HttpRoutes[IO] =
    HttpRoutes[IO] { request =>
      val requestInfo = for {
        contentType <- request.headers
          .get[`Content-Type`]
          .map(ct => s"${ct.mediaType.mainType}/${ct.mediaType.subType}")
        userAgent <- request.headers.get[`User-Agent`].map(_.product.toString)
      } yield RequestInfo(
        contentType,
        userAgent
      )
      OptionT.liftF(local.set(requestInfo)) *> routes(request)
    }

}
