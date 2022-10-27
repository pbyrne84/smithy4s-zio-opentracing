import cats.effect.{IO, IOLocal}
import cats.effect.kernel.Resource
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import smithy4s.hello.GenericBadRequestError

object Routes {
  private val docs =
    smithy4s.http4s.swagger.docs[IO](smithy4s.hello.HelloWorldService)

  def getAll(local: IOLocal[Option[RequestInfo]]): Resource[IO, HttpRoutes[IO]] = {
    val eventualRequestInfo = createEventualMaybeRequestInfo(local)

    smithy4s.http4s.SimpleRestJsonBuilder
      .routes(new HelloWorldServiceImpl(eventualRequestInfo))
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
      local: IOLocal[Option[RequestInfo]]
  ): IO[RequestInfo] = {
    local.get.flatMap {
      case Some(value) => IO.pure(value)
      case None =>
        IO.raiseError(
          new IllegalAccessException(
            "Tried to access the value outside of the lifecycle of an http request"
          )
        )
    }
  }
}
