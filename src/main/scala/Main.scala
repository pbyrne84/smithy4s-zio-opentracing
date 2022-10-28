import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server._
import org.http4s.headers.`X-B3-TraceId`
import org.http4s.implicits._
import trace.{B3Companion, B3ParentSpanId, B3Sampled, B3SpanId, B3TraceId, B3Value}

case class RequestInfo(
    maybeContentType: Option[String] = None,
    maybeUserAgent: Option[String] = None,
    maybeB3TraceId: Option[B3TraceId] = None,
    maybeB3SpanId: Option[B3SpanId] = None,
    maybeB3ParentSpanId: Option[B3ParentSpanId] = None,
    maybeB3Sampled: Option[B3Sampled] = None
) {

  val kernalHeaders: Map[String, String] = List(
    maybeB3TraceId,
    maybeB3SpanId,
    maybeB3ParentSpanId,
    maybeB3Sampled
  ).collect { case Some(b3HeaderValue: B3Value[_]) =>
    b3HeaderValue.name -> b3HeaderValue.stringValue
  }.toMap

}

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
