package zioexample

import cats.data.OptionT
import org.http4s.{HttpRoutes, Request}
import org.typelevel.ci.CIString
import zio.{FiberRef, Task}

object ZIOMiddleware {

  import zio.interop.catz.asyncInstance

  def withRequestInfo(
      routes: HttpRoutes[Task],
      requestFibreRef: FiberRef[Request[Task]]
  ): HttpRoutes[Task] =
    HttpRoutes[Task] { request: Request[Task] =>
      val requestOnFibreRefForRequest: OptionT[Task, Unit] =
        OptionT.liftF(requestFibreRef.set(request))

      import cats.implicits.catsSyntaxApply
      requestOnFibreRefForRequest *> routes(request)
    }

}
