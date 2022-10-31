import cats.effect.{IO, IOLocal}
import natchez.Trace
import smithy4s.hello.{Greeting, HelloWorldService}

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
