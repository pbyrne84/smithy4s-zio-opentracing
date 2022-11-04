import cats.Monad
import cats.data.Kleisli
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import cats.effect.{Async, IO, IOLocal, Resource}
import org.http4s.CharsetRange.*
import scalaz.Applicative
import smithy4s.hello.{Greeting, HelloWorldService, HelloWorldServiceGen}
import trace4cats.avro.AvroSpanCompleter
import trace4cats.kernel.SpanSampler
import trace4cats.{CompleterConfig, EntryPoint, Span, Trace, TraceProcess}

import scala.concurrent.duration.DurationInt

final class HelloWorldServiceImpl(requestInfoEffect: IO[RequestInfo])
    extends HelloWorldService[IO] {

  def hello(
      name: String,
      town: Option[String]
  ): IO[Greeting] = {
    def createGreeting(str: String): Greeting =
      Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

    for {
      requestInfo <- requestInfoEffect
      local <- IOLocal(42)
      _ <- IO.println("REQUEST_INFO: " + requestInfo)
      greeting <- IO.pure {
        town match {
          case None =>
            createGreeting(s"Hello $name!")
          case Some(t) =>
            createGreeting(s"Hello $name from $t!")
        }
      }

    } yield greeting
  }

}

class HelloServiceWithTracing {

  def hello[F[_]: Monad: Trace: Console](
      requestInfoEffect: F[RequestInfo],
      name: String,
      town: Option[String]
  ): F[Greeting] = {
    import cats.implicits._

    for {
      requestInfo <- requestInfoEffect
      _ <- Trace[F].span("span1") {
        Console[F].println("trace this operation")
      }
      traceId <- Trace[F].traceId
      _ <- Console[F].println(traceId)
      greeting =
        town match {
          case None =>
            createGreeting(s"Hello $name!")
          case Some(t) =>
            createGreeting(s"Hello $name from $t!")
        }
    } yield greeting
  }

  private def xSpan[F[_]: Monad: Trace: Console] = {

    for {

      a <- Trace[F].span("kamon-cats-io-3")

    } yield ()
  }

  private def createGreeting(name: String): Greeting =
    Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

}

final class HelloWorldServiceImpl2(requestInfoEffect: IO[RequestInfo])
    extends HelloWorldService[IO] {

  def hello(
      name: String,
      town: Option[String]
  ): IO[Greeting] = {

    import trace4cats._

    entryPoint[IO](TraceProcess("trace4cats")).use { ep =>
      ep.root("this is the root span").use { span =>

        val someInt: Option[Int] = Some(1)

        val helloServiceWithTracing = new HelloServiceWithTracing()

        val a = Kleisli[IO, Span[IO], RequestInfo](_ => requestInfoEffect)
        val b = Kleisli[Option, String, Int](_ => someInt)

        helloServiceWithTracing
          .hello[Kleisli[IO, Span[IO], *]](a, name, town)
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
