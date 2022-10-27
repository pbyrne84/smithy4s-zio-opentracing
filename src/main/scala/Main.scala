import smithy4s.hello._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http.MetadataError.FailedConstraint
import smithy4s.http4s.SimpleRestJsonBuilder

object HelloWorldImpl extends HelloWorldService[IO] {
  def hello(
      name: String,
      town: Option[String]
  ): IO[Greeting] = IO
    .pure {

      def createGreeting(str: String): Greeting =
        Greeting(s"Hello $name!", "xxx", "xxxx", "xxx", "1")

      town match {
        case None => createGreeting(s"Hello $name!")
        case Some(t) => createGreeting(s"Hello $name from $t!")
      }
    }
}

object Routes {
  private val example: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder
      .routes(HelloWorldImpl)
      .mapErrors { case e: FailedConstraint =>
        println(e)
        GenericBadRequestError("", "", "", "", message = Some(s"Hello $e!"))
      }
      .resource

  private val docs: HttpRoutes[IO] =
    smithy4s.http4s.swagger.docs[IO](HelloWorldService)

  val all: Resource[IO, HttpRoutes[IO]] = example.map(_ <+> docs)
}

object Main extends IOApp.Simple {

  val run = Routes.all
    .flatMap { routes =>
      EmberServerBuilder
        .default[IO]
        .withPort(port"9000")
        .withHost(host"localhost")
        .withHttpApp(routes.orNotFound)
        .build
    }
    .use(_ => IO.never)

}
