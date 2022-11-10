import org.http4s.{HttpRoutes, Response}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2

import zio.{Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault}

object ZIOMain extends ZIOAppDefault {

  //  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
//    ZIO.runtime.flatMap { implicit r: Runtime[Clock] =>
//      ZIORoutes.getAll.flatMap { routes: HttpRoutes[Task] =>
//        import zio.interop.catz.core._
//
//        val F: cats.effect.Async[Task] = implicitly
//
//        EmberServerBuilder
//          .default[Task]
//          .withHost(host"localhost")
//          .withPort(port"9000")
//          .withHttpApp(wsb => routes)
//          .orNotFound
//          .build
//      }.useForever
//
//    }
//
//  }
//
//  val serve: Task[Unit] =
//    ZIORoutes.getAll.flatMap { routes: HttpRoutes[Task] =>
//      ZIO.executor.flatMap(executor =>
//        BlazeServerBuilder[Task]
//          .withExecutionContext(executor.asExecutionContext)
//          .bindHttp(8080, "localhost")
//          .withHttpWebSocketApp(wsb => routes).orNotFound)
//          .serve
//          .compile
//          .drain
//      )
//    }

//  def run2 = IOLocal(RequestInfo()).flatMap { local: IOLocal[RequestInfo] =>
//    IORoutes
//      .getAll(local)
//      .flatMap { routes =>
//        EmberServerBuilder
//          .default[Task]
//          .withHost(host"localhost")
//          .withPort(port"9000")
//          .withHttpApp(routes.orNotFound)
//          .build
//      }
//      .useForever
//  }

//  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = serve.exitCode
//  import zio.interop.catz._
//  import cats.implicits._
//  import org.http4s._
//  import cats.syntax.all._

//  val serve: Task[Unit] = {
//    val F: cats.effect.Async[Task] = implicitly
//
//    import zio.interop.catz.core._
//
//    ZIO.executor.flatMap((executor: Executor) =>
//      BlazeServerBuilder[Task]
//        .withExecutionContext(executor.asExecutionContext)
//        .bindHttp(8080, "localhost")
//        .withHttpApp(null)
//        .serve
//        .compile
//        .drain
//
////      ZIORoutes.getAll.flatMap { routes =>
////
////      }
//    )
//  }

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = ???

  val serve: Task[Unit] = {
    import zio.interop.catz._
    import zio.interop.catz.implicits.rts

    import org.http4s._
    ZIO.executor.flatMap(executor =>
      BlazeServerBuilder[Task]
        .withExecutionContext(executor.asExecutionContext)
        .bindHttp(8080, "localhost")
        .withHttpWebSocketApp(wsb => Router("/" -> ZIO.succeed(Response(Status.Ok))).orNotFound)
        .serve
        .compile
        .drain
    )
  }

}
