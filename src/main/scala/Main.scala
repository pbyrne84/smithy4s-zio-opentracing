import smithy4s.hello._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http.MetadataError.FailedConstraint
import smithy4s.http4s.SimpleRestJsonBuilder

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
