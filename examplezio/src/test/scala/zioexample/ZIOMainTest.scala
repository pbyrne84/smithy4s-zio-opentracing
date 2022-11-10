package zioexample

import zio.ZIO
import zio.test._

import java.time.Duration

object ZIOMainTest extends ZIOSpecDefault {
  override def spec = suite("x")(
    test("dsss") {
      for {
        service <- startService
        _ <- service.interrupt
      } yield assertTrue(true == true)
    }
  )

  private def startService = {
    for {
      service <- ZIOMain.initialiseBlaze.fork
      _ <- ZIO.sleep(Duration.ofMillis(2000))
    } yield service
  }
}
