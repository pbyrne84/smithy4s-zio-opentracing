package exampleio.natchezexample

import cats.data.Kleisli
import cats.effect.{IO, Resource, Sync}
import io.jaegertracing.Configuration
import io.jaegertracing.internal.JaegerTracer
import natchez.{EntryPoint, Span}
import natchez.jaeger.Jaeger
import org.http4s.CharsetRange.*
import smithy4s.hello.{
  Greeting,
  HelloWorldService,
  PersonResponse,
  PersonUpdate,
  PersonUpdatePayload
}
import trace.RequestInfo

class NatchezCatsHelloWorldService(requestInfoEffect: IO[RequestInfo])
    extends HelloWorldService[IO] {

  override def hello(name: String, town: Option[String]): IO[Greeting] = ???

//  override def hello(name: String, town: Option[String]): IO[Greeting] = {
//    entryPoint.use { ep: EntryPoint[IO] =>
//
//      val value = ep.root("aaaa").use { span => }
//
//      val natchezCatsHelloServiceWithTracing = new NatchezCatsHelloServiceWithTracing()
//      val getRequestKleisli: Kleisli[IO, Span[IO], RequestInfo] =
//        Kleisli[IO, Span[IO], RequestInfo](_ => requestInfoEffect)
//
//      natchezCatsHelloServiceWithTracing
//        .hello[Kleisli[IO, Span[IO], *]](getRequestKleisli, ep, name, town)
//
//    }
//  }
//
//  private def entryPoint: Resource[IO, EntryPoint[IO]] = {
//    Jaeger.entryPoint[IO]("", None) { configuration =>
//
//      val builder = new JaegerTracer.Builder("sss")
//
//      IO.pure(builder.build())
//
//    }
//  }
//
//  private def createGreeting(name: String): Greeting =
//    Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

  override def updatePerson(data: PersonUpdatePayload): IO[PersonResponse] = ???
}
