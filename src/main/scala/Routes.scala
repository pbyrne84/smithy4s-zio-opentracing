import cats.effect.{IO, IOLocal}
import cats.effect.kernel.Resource
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import smithy4s.hello.GenericBadRequestError
import trace.RequestInfo
import trace4catsexample.Trace4CatsHelloWorldService

object Routes {
  private val docs =
    smithy4s.http4s.swagger.docs[IO](smithy4s.hello.HelloWorldService)

  def getAll(local: IOLocal[RequestInfo]): Resource[IO, HttpRoutes[IO]] = {
    val eventualRequestInfo = createEventualMaybeRequestInfo(local)

    smithy4s.http4s.SimpleRestJsonBuilder
      .routes(new Trace4CatsHelloWorldService(eventualRequestInfo))
      .flatMapErrors { case error =>
        eventualRequestInfo.map { requestInfo =>
          new GenericBadRequestError("", "", "", "", "bananananana " + error + requestInfo)
        }
      }
      .resource
      .map { routes =>
        Middleware.withRequestInfo(routes <+> docs, local)
      }
  }

  private def createEventualMaybeRequestInfo(
      local: IOLocal[RequestInfo]
  ): IO[RequestInfo] = {
    local.get.flatMap { value =>
      IO.pure(value)

    }
  }
}
