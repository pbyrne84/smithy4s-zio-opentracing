import cats.Monad
import cats.data.Kleisli
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

  def hello[F[_]: Monad: Trace](
      requestInfoEffect: F[RequestInfo],
      name: String,
      town: Option[String]
  ): F[Greeting] = {
    import cats.implicits._

    for {
      requestInfo <- requestInfoEffect
      greeting =
        town match {
          case None =>
            createGreeting(s"Hello $name!")
          case Some(t) =>
            createGreeting(s"Hello $name from $t!")
        }
    } yield greeting
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

    def runF[F[_]: Monad: Trace] = {}

    import cats.implicits._
    import trace4cats._

    entryPoint[IO](TraceProcess("trace4cats")).use { ep =>
      ep.root("this is the root span").use { span =>

        val helloServiceWithTracing = new HelloServiceWithTracing()
        helloServiceWithTracing
          .hello[Kleisli[IO, Span[IO], *]](requestInfoEffect, name, town)
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
