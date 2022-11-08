import cats.Monad
import cats.data.Kleisli
import cats.effect._
import cats.effect.std.Console
import cats.implicits._
import trace4cats.model.{SpanKind, TraceProcess}
import trace4cats.{CompleterConfig, EntryPoint, Span, SpanStatus}
import trace4cats.avro.AvroSpanCompleter
import trace4cats.kernel.SpanSampler
import trace4cats.Trace

import scala.concurrent.duration._

object Trace4CatsQuickStart extends IOApp.Simple {

  def run: IO[Unit] =
    entryPoint[IO](TraceProcess("trace4cats")).use { ep =>
      ep.root("this is the root span").use { span =>
        runF[Kleisli[IO, Span[IO], *]].run(span)
      }
    }

  private def entryPoint[F[_]: Async](process: TraceProcess): Resource[F, EntryPoint[F]] =
    AvroSpanCompleter.udp[F](process, config = CompleterConfig(batchTimeout = 50.millis)).map {
      completer =>
        EntryPoint[F](SpanSampler.always[F], completer)
    }

  private def runF[F[_]: Monad: Console: Trace]: F[Unit] =
    for {
      _ <- Trace[F].span("span1")(Console[F].println("trace this operation"))
      _ <- Trace[F].span("span2", SpanKind.Client)(Console[F].println("send some request"))
      _ <- Trace[F].span("span3", SpanKind.Client)(
        Trace[F].putAll("attribute1" -> "test", "attribute2" -> 200) >>
          Trace[F].setStatus(SpanStatus.Cancelled)
      )
    } yield ()
}
