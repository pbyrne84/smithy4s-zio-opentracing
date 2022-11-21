package exampleio

import cats.effect._
import com.comcast.ip4s._
import org.http4s.Request
import org.http4s.ember.server._
import trace.RequestInfo

object IOMain extends IOApp.Simple {

  def run: IO[Unit] = IOLocal(Request[IO]()).flatMap { ioLocalRequestInfo =>
    IORoutes
      .getAll(ioLocalRequestInfo)
      .flatMap { routes =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"localhost")
          .withPort(port"8080")
          .withHttpApp(routes.orNotFound)
          .build
      }
      .useForever
  }
}
