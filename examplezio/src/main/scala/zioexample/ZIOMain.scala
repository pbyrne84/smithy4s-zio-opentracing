package zioexample

import org.http4s.Request
import org.http4s.blaze.server.BlazeServerBuilder
import zio.{FiberRef, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

object ZIOMain extends ZIOAppDefault {

  private val emptyRequest: Request[Task] = Request[Task]()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    initialiseBlaze

  }

  def initialiseBlaze: ZIO[Scope, Throwable, Unit] = {
    // I always put implicit imports close to use as it makes things easier to understand and also easy to copy.
    // If they are used throughout the class then I would put them under the class definition so it is not
    // lost in the main imports. Implicits can be a large barrier to entry.
    // Intellij found **zio.interop.catz.implicits.rts** for me.
    import zio.interop.catz._
    import zio.interop.catz.implicits.rts

    FiberRef.make(emptyRequest).flatMap { emptyRequestFibreRef: FiberRef[Request[Task]] =>
      ZIO.executor
        .flatMap { executor =>
          ZIORoutes.getAll(emptyRequestFibreRef).toScopedZIO.flatMap { routes =>
            BlazeServerBuilder[Task]
              .withExecutionContext(executor.asExecutionContext)
              .bindHttp(8080, "localhost")
              .withHttpWebSocketApp(_ => routes.orNotFound)
              .serve
              .compile
              .drain
          }
        }
        .provide(Scope.default)
    }
  }
}
