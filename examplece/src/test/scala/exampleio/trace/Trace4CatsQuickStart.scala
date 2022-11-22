package exampleio.trace

import cats.Monad
import cats.data.Kleisli
import cats.effect._
import cats.effect.std.Console
import cats.implicits._
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.{LoggerFactory, SelfAwareStructuredLogger}
import trace4cats.Trace.WithContext
import trace4cats.{Trace, _} // trace4cats._ can fighter
import trace4cats.avro.AvroSpanCompleter

import scala.concurrent.duration._

object Trace4CatsQuickStart extends IOApp.Simple {

  implicit val logging: LoggerFactory[IO] = Slf4jFactory[IO]

  def run: IO[Unit] =
    entryPoint[IO](TraceProcess("trace4cats")).use { ep =>
      ep.root("this is the root span").use { span: Span[IO] =>
        runF[Kleisli[IO, Span[IO], *]].run(span)
      }
    }

  private def entryPoint[F[_]: Async](process: TraceProcess): Resource[F, EntryPoint[F]] =
    AvroSpanCompleter.udp[F](process, config = CompleterConfig(batchTimeout = 50.millis)).map {
      completer =>
        EntryPoint[F](SpanSampler.always[F], completer)
    }

  // WithContext allows getting context from trace simply using Trace does not
  private def runF[F[_]: Sync: Monad: Console: WithContext]: F[Unit] =
    for {
      _ <- Trace[F].span("span1")(x)
      context <- Trace[F].context
      _ <- Console[F].println(context)
      _ <- Trace[F].span("span2", SpanKind.Client)(Console[F].println("send some request"))
      _ <- Trace[F].span("span3", SpanKind.Client)(
        Trace[F].putAll("attribute1" -> "test", "attribute2" -> 200) >>
          Trace[F].setStatus(SpanStatus.Cancelled)
      )
    } yield ()

  private def x[F[_]: Sync: Console: WithContext]: F[true] = {
    val logger = anyFSyncLogger
    for {
      _ <- Console[F].println("trace this operation")
      context <- Trace[F].context
      _ <- Console[F].println(context)
      _ <- logger.info("moo")
    } yield true
  }

  def anyFSyncLogger[F[_]: Sync]: SelfAwareStructuredLogger[F] = Slf4jFactory[F].getLogger
}
