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
      ioLocalRequestInfo: IOLocal[Request[IO]]
  ): HttpRoutes[IO] =
    HttpRoutes[IO] { request: Request[IO] =>

      val updatedLocalRequestInfo: OptionT[IO, Unit] =
        OptionT.liftF(ioLocalRequestInfo.set(request))

      updatedLocalRequestInfo *> routes(request)
    }

}
