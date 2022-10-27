import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server._
import org.http4s.headers.`X-B3-TraceId`
import org.http4s.implicits._
import trace.B3TraceId

case class RequestInfo(
    maybeContentType: Option[String] = None,
    maybeUserAgent: Option[String] = None,
    maybeB3TraceId: Option[B3TraceId] = None,
    maybeB3SpanId: Option[String] = None,
    maybeB3ParentSpanId: Option[String] = None,
    maybeB3Sampled: Option[String] = None
) {}

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
