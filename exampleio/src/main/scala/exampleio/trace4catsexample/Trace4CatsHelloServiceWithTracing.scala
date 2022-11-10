package exampleio.trace4catsexample

import cats.Monad
import cats.effect.std.Console
import smithy4s.hello.Greeting
import trace.RequestInfo
import trace4cats.Trace

class Trace4CatsHelloServiceWithTracing {

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

  private def createGreeting(name: String): Greeting =
    Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

}
