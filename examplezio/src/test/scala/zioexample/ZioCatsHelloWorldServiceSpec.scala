package zioexample

import org.http4s.Request
import zio.test._
import zio.{Scope, ZIO}

object ZioCatsHelloWorldServiceSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] = {

    val service = new ZioHelloWorldService(ZIO.succeed(Request()))

    suite("ZioCatsHelloWorldService") {
      test("xxxx") {
        for {
          _ <- service.hello("moo", None)
        } yield assertTrue(true)
      }
    }

  }
}
