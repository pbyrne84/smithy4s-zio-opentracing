import cats.Monad
import cats.effect.{IO, IOLocal}
import natchez.Trace
import scalaz.Applicative
import smithy4s.hello.{Greeting, HelloWorldService, HelloWorldServiceGen}

final class HelloWorldServiceImpl(requestInfoEffect: IO[RequestInfo])
    extends HelloWorldService[IO] {

  def hello(
      name: String,
      town: Option[String]
  ): IO[Greeting] = {
    def createGreeting(str: String): Greeting =
      Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

    for {
      requestInfo <- requestInfoEffect
      local <- IOLocal(42)
      _ <- IO.println("REQUEST_INFO: " + requestInfo)
      greeting <- IO.pure {
        town match {
          case None =>
            createGreeting(s"Hello $name!")
          case Some(t) =>
            createGreeting(s"Hello $name from $t!")
        }
      }

    } yield greeting
  }

}

class HelloServiceWithTracing {

  def hello[F[_]: Monad: Trace](
      requestInfoEffect: F[RequestInfo],
      name: String,
      town: Option[String]
  ): F[Greeting] = {
    import cats.implicits._

    for {
      requestInfo <- requestInfoEffect
      greeting =
        town match {
          case None =>
            createGreeting(s"Hello $name!")
          case Some(t) =>
            createGreeting(s"Hello $name from $t!")
        }
    } yield greeting
  }

  private def createGreeting(name: String): Greeting =
    Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

}
