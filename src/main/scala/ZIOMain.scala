import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server._
import org.http4s.implicits._
import trace.RequestInfo
import zio.Task
import zio._

object ZIOMain extends ZIOAppDefault {
  import zio.interop.catz._

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    ZIO.attempt(
      ZIORoutes.getAll.flatMap { routes =>
        EmberServerBuilder
          .default[Task]
          .withHost(host"localhost")
          .withPort(port"9000")
          .withHttpApp(routes.orNotFound)
          .build
      }.useForever
    )

  }

  def run2 = IOLocal(RequestInfo()).flatMap { local: IOLocal[RequestInfo] =>
    IORoutes
      .getAll(local)
      .flatMap { routes =>
        EmberServerBuilder
          .default[Task]
          .withHost(host"localhost")
          .withPort(port"9000")
          .withHttpApp(routes.orNotFound)
          .build
      }
      .useForever
  }
}
