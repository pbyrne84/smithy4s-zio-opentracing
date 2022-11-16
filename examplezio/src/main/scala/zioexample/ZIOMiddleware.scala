package zioexample

import cats.data.OptionT
import org.http4s.{HttpRoutes, Request}
import zio.{FiberRef, Task}

object ZIOMiddleware {

  def withRequestInfo(
      routes: HttpRoutes[Task],
      requestFibreRef: FiberRef[Request[Task]]
  ): HttpRoutes[Task] = {
    import zio.interop.catz.asyncInstance

    HttpRoutes[Task] { request: Request[Task] =>
      val requestOnFibreRefForRequest: OptionT[Task, Unit] =
        OptionT.liftF(requestFibreRef.set(request))

      import cats.implicits.catsSyntaxApply
      requestOnFibreRefForRequest *> routes(request)
    }
  }

}
