import cats.effect.IO
import smithy4s.hello.{Greeting, HelloWorldService}

final class HelloWorldServiceImpl(requestInfoEffect: IO[RequestInfo])
    extends HelloWorldService[IO] {

  def hello(
      name: String,
      town: Option[String]
  ): IO[Greeting] = {
    def createGreeting(str: String): Greeting =
      Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

    requestInfoEffect.flatMap { requestInfo =>
      IO.println("REQUEST_INFO: " + requestInfo)
        .as {
          town match {
            case None => createGreeting(s"Hello $name!")
            case Some(t) => createGreeting(s"Hello $name from $t!")
          }
        }
    }
  }
}
