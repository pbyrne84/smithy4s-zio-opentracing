package trace4catsexample

import cats.data.Kleisli
import cats.effect.{Async, IO, Resource}
import org.http4s.CharsetRange.*
import smithy4s.hello.{Greeting, HelloWorldService}
import trace.RequestInfo
import trace4cats.{CompleterConfig, EntryPoint, Span}
import trace4cats.avro.AvroSpanCompleter
import trace4cats.kernel.SpanSampler
import trace4cats.model.TraceProcess

import scala.concurrent.duration.DurationInt

final class Trace4CatsHelloWorldService(requestInfoEffect: IO[RequestInfo])
    extends HelloWorldService[IO] {

  def hello(
      name: String,
      town: Option[String]
  ): IO[Greeting] = {

    entryPoint[IO](TraceProcess("trace4cats")).use { ep: EntryPoint[IO] =>
      ep.root("this is the root span").use { span =>
        val helloServiceWithTracing = new Trace4CatsHelloServiceWithTracing()
        val getRequestKleisli = Kleisli[IO, Span[IO], RequestInfo](_ => requestInfoEffect)

        helloServiceWithTracing
          .hello[Kleisli[IO, Span[IO], *]](getRequestKleisli, name, town)
          .run(span)

      }
    }

  }

  private def entryPoint[F[_]: Async](process: TraceProcess): Resource[F, EntryPoint[F]] =
    AvroSpanCompleter.udp[F](process, config = CompleterConfig(batchTimeout = 50.millis)).map {
      completer =>
        EntryPoint[F](SpanSampler.always[F], completer)
    }

}
