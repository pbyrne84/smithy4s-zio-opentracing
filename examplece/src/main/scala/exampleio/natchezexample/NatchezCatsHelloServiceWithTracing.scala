package exampleio.natchezexample

import cats.Monad
import cats.effect.{IO, Resource}
import cats.effect.kernel.Sync
import cats.effect.std.Console
import io.jaegertracing.internal.JaegerTracer
import natchez.jaeger.Jaeger
import natchez.{EntryPoint, Span, Trace}
import org.http4s.Request
import smithy4s.hello.Greeting

class NatchezCatsHelloServiceWithTracing(jaegerTracer: JaegerTracer) {
  // import natchez.http4s.implicits._

  def hello(
      requestInfoEffect: IO[Request[IO]],
      name: String,
      town: Option[String]
  ): IO[Greeting] = {

    //   import cats.implicits._

    entryPoint.use { ep: EntryPoint[IO] =>
      ep.root("aaaa").use { span =>
        for {
          request <- requestInfoEffect
          _ = println(request)
        } yield town
          .map(t => createGreeting(s"Hello $name from $t!"))
          .getOrElse(createGreeting(s"Hello $name!"))
      }

    }

  }

  private def entryPoint: Resource[IO, EntryPoint[IO]] = {
    Jaeger.entryPoint[IO]("thundercats", None) { configuration =>
      IO.pure(jaegerTracer)

    }
  }

  private def createGreeting(name: String): Greeting =
    Greeting(s"Hello $name!", "xxx", "xxxx", "1")

}
