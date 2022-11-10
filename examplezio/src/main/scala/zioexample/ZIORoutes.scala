package zioexample

import cats.effect.kernel.Resource
import org.http4s.{HttpRoutes, Request}
import org.typelevel.ci.CIString
import zio.{FiberRef, Task, UIO, ZIO}

object ZIORoutes {

  import zio.interop.catz._
  import zio.interop.catz.implicits.rts

  private val docs =
    smithy4s.http4s.swagger.docs[Task](smithy4s.hello.HelloWorldService)

  def getAll(
      emptyRequestFibreRef: FiberRef[Request[Task]]
  ): Resource[Task, HttpRoutes[Task]] = {

    smithy4s.http4s.SimpleRestJsonBuilder
      .routes(new ZioCatsHelloWorldService(createEventualRequest(emptyRequestFibreRef)))
      .resource
      .map { routes =>
        import cats.implicits.toSemigroupKOps
        ZIOMiddleware.withRequestInfo(routes <+> docs, emptyRequestFibreRef)
      }
  }

  private def createEventualRequest(
      ioLocalHeaders: FiberRef[Request[Task]]
  ): UIO[Request[Task]] = {
    ioLocalHeaders.get
  }
}
