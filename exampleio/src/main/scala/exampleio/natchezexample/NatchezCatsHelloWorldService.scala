package exampleio.natchezexample

import cats.effect.IO
import io.jaegertracing.internal.JaegerTracer
import org.http4s.Request
import smithy4s.hello.{Greeting, HelloWorldService}
import trace.RequestInfo

class NatchezCatsHelloWorldService(requTestIO: IO[Request[IO]]) extends HelloWorldService[IO] {

  private val jaegerTracer: JaegerTracer = new JaegerTracer.Builder("sss").build()
  private val natchezCatsHelloServiceWithTracing = new NatchezCatsHelloServiceWithTracing(
    jaegerTracer
  )

  override def hello(name: String, town: Option[String]): IO[Greeting] = {


    natchezCatsHelloServiceWithTracing.hello(requTestIO, name, town)
  }

}
