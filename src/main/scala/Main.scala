import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server._
import org.http4s.implicits._

case class RequestInfo(contentType: String, userAgent: String)

object Main extends IOApp.Simple {

  def run: IO[Unit] = IOLocal(Option.empty[RequestInfo]).flatMap { local =>
    Routes
      .getAll(local)
      .flatMap { routes =>
        EmberServerBuilder
          .default[IO]
          .withHost(host"localhost")
          .withPort(port"9000")
          .withHttpApp(routes.orNotFound)
          .build
      }
      .useForever
  }
}
