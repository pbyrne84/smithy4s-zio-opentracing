package natchezexample

import cats.Monad
import cats.effect.std.Console
import natchez.Trace
import smithy4s.hello.Greeting
import trace.RequestInfo

class NatchezCatsHelloServiceWithTracing {

  def hello[F[_]: Monad: Trace: Console](
      requestInfoEffect: F[RequestInfo],
      name: String,
      town: Option[String]
  ): F[Greeting] = ???

}
