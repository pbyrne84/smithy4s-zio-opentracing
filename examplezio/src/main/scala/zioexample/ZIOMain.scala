package zioexample

import org.http4s.{HttpRoutes, Request}
import org.http4s.blaze.server.BlazeServerBuilder
import zio.{FiberRef, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

import scala.concurrent.ExecutionContext

object ZIOMain extends ZIOAppDefault {

  private val emptyRequest: Request[Task] = Request[Task]()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    FiberRef.make(emptyRequest).flatMap { emptyRequestFibreRef: FiberRef[Request[Task]] =>
      ZIO.executor
        .flatMap { executor =>
          getRoutes(emptyRequestFibreRef).flatMap { routes: HttpRoutes[Task] =>
            createBlazeServer(executor.asExecutionContext, routes)
          }
        }
        .provide(Scope.default)
    }
  }

  // public as the routes is what we will test against
  def getRoutes(
      emptyRequestFibreRef: FiberRef[Request[Task]]
  ): ZIO[Any with Scope, Throwable, HttpRoutes[Task]] = {
    import zio.interop.catz._

    ZIORoutes.getAll(emptyRequestFibreRef).toScopedZIO
  }

  private def createBlazeServer(
      executionContext: ExecutionContext,
      routes: HttpRoutes[Task]
  ): Task[Unit] = {
    import zio.interop.catz.implicits.rts
    import zio.interop.catz._

    BlazeServerBuilder[Task]
      .withExecutionContext(executionContext)
      .bindHttp(8080, "localhost")
      .withHttpWebSocketApp(_ => routes.orNotFound)
      .serve
      .compile
      .drain
  }
}
