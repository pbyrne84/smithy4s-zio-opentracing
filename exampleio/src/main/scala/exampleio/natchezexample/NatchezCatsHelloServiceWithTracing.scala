package exampleio.natchezexample

import cats.Monad
import cats.effect.IO
import cats.effect.kernel.Sync
import cats.effect.std.Console
import io.jaegertracing.internal.JaegerTracer
import natchez.jaeger.Jaeger
import natchez.{EntryPoint, Span, Trace}
import smithy4s.hello.Greeting
import trace.RequestInfo

class NatchezCatsHelloServiceWithTracing {

  def hello[F[_]: Monad: Trace: Console: Sync](
      requestInfoEffect: F[RequestInfo],
      ep: EntryPoint[F],
      name: String,
      town: Option[String]
  ): F[Greeting] = {

    import cats.implicits._

    for {
      requestInfo <- requestInfoEffect

//      greeting <- natchez.Trace[F].span("aa") {
//        town
//          .map(t => createGreeting(s"Hello $name from $t!"))
//          .getOrElse(createGreeting(s"Hello $name!"))
//      }

      greeting = town
        .map(t => createGreeting(s"Hello $name from $t!"))
        .getOrElse(createGreeting(s"Hello $name!"))

    } yield greeting

  }

  private def createGreeting(name: String): Greeting =
    Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

}
