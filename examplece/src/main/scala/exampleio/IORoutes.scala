package exampleio

import cats.effect.{IO, IOLocal}
import cats.effect.kernel.Resource
import exampleio.natchezexample.NatchezCatsHelloWorldService
import org.http4s.{HttpRoutes, Request}
import smithy4s.hello.GenericBadRequestError

object IORoutes {
  private val docs =
    smithy4s.http4s.swagger.docs[IO](smithy4s.hello.HelloWorldService)

  def getAll(ioLocalRequestInfo: IOLocal[Request[IO]]): Resource[IO, HttpRoutes[IO]] = {
    val eventualRequestInfo = createEventualRequestInfo(ioLocalRequestInfo)

    import cats.implicits.toSemigroupKOps

    smithy4s.http4s.SimpleRestJsonBuilder
      .routes(new NatchezCatsHelloWorldService(eventualRequestInfo))
      .flatMapErrors { case error =>
        eventualRequestInfo.map { requestInfo =>
          new GenericBadRequestError("", "", "", "bananananana " + error + requestInfo)
        }
      }
      .resource
      .map { routes =>
        IOMiddleware.withRequestInfo(routes <+> docs, ioLocalRequestInfo)
      }
  }

  private def createEventualRequestInfo(
      ioLocalRequestInfo: IOLocal[Request[IO]]
  ) = {
    ioLocalRequestInfo.get.flatMap { request =>
      IO.pure(request)

    }
  }
}
