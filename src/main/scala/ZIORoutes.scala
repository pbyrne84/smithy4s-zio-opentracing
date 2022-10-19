import cats.effect.{IO, IOLocal}
import cats.effect.kernel.Resource
import cats.implicits.toSemigroupKOps
import org.http4s.HttpRoutes
import smithy4s.hello.GenericBadRequestError
import trace.RequestInfo
import trace4catsexample.Trace4CatsHelloWorldService
import zio.Task
import zioexample.ZioCatsHelloWorldService

object ZIORoutes {
//  private val docs =
//    smithy4s.http4s.swagger.docs[Task](smithy4s.hello.HelloWorldService)

  def getAll: Resource[Task, HttpRoutes[Task]] = {
    import zio.interop.catz._
    import zio.interop.catz.implicits.rts

    smithy4s.http4s.SimpleRestJsonBuilder
      .routes(new ZioCatsHelloWorldService)
      .resource
      .map { routes =>
        ZIOMiddleware.withRequestInfo(routes)
      }
  }

}
