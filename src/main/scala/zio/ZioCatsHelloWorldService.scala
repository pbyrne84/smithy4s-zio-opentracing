package zio

import cats.effect.IO
import smithy4s.hello.{Greeting, HelloWorldService}

import java.util
class ZioCatsHelloWorldService extends HelloWorldService[Task] {

  def hello(
      name: String,
      town: Option[String]
  ): Task[Greeting] = {

    ZIO.attempt(
      town
        .map(t => createGreeting(s"Hello $name from $t!"))
        .getOrElse(createGreeting(s"Hello $name!"))
    )
  }

  private def createGreeting(name: String): Greeting =
    Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

}
