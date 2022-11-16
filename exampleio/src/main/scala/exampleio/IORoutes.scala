package exampleio

import cats.effect.{IO, IOLocal}
import cats.effect.kernel.Resource

import org.http4s.HttpRoutes
import smithy4s.hello.GenericBadRequestError
import trace.RequestInfo
import trace4catsexample.Trace4CatsHelloWorldService

object IORoutes {
  private val docs =
    smithy4s.http4s.swagger.docs[IO](smithy4s.hello.HelloWorldService)

  def getAll(ioLocalRequestInfo: IOLocal[RequestInfo]): Resource[IO, HttpRoutes[IO]] = {
    val eventualRequestInfo = createEventualRequestInfo(ioLocalRequestInfo)

    import cats.implicits.toSemigroupKOps

    smithy4s.http4s.SimpleRestJsonBuilder
      .routes(new Trace4CatsHelloWorldService(eventualRequestInfo))
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
      ioLocalRequestInfo: IOLocal[RequestInfo]
  ): IO[RequestInfo] = {
    ioLocalRequestInfo.get.flatMap { requestInfo: RequestInfo =>
      IO.pure(requestInfo)

    }
  }
}
