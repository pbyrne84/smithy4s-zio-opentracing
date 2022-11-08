import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server._
import org.http4s.headers.`X-B3-TraceId`
import org.http4s.implicits._
import trace.{B3Companion, B3ParentSpanId, B3Sampled, B3SpanId, B3TraceId, B3Value, RequestInfo}

object Main extends IOApp.Simple {

  def run: IO[Unit] = IOLocal(RequestInfo()).flatMap { local =>
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
