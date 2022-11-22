package exampleio.trace

import cats.effect.{IO, IOLocal}
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

import java.util
class FibreRefBehaviourSpec extends AsyncWordSpec with AsyncIOSpec with Matchers {
  "x" should {
    "y" in {

      val eventualLocal: IO[IOLocal[Map[String, String]]] = IOLocal(Map.empty[String, String])

      for {
        local <- eventualLocal
        map1 <- forkTest("a", "apple", local)
        map2 <- forkTest("b", "banana", local)
      } yield {
        println(map1)
        println(map2)
      }

    }
  }

  import cats.Monad
  import cats.data.Kleisli
  import cats.effect._
  import cats.effect.std.Console
  import cats.implicits._
  import trace4cats._
  import trace4cats.avro.AvroSpanCompleter

  import scala.concurrent.duration._

  def forkTest(
      newKey: String,
      newValue: String,
      ioLocal: IOLocal[Map[String, String]]
  ): IO[Map[String, String]] = {

    val value1 = IO.deferred[IO[Map[String, String]]]

    ???
  }

  def entryPoint(process: TraceProcess): Resource[IO, EntryPoint[IO]] =
    AvroSpanCompleter.udp[IO](process, config = CompleterConfig(batchTimeout = 50.millis)).map {
      completer =>
        EntryPoint[IO](SpanSampler.always[IO], completer)
    }

  def runF: IO[Unit] = {
    import trace4cats.Trace.Implicits.noop

    for {

      _ <- Trace[IO].span("span1")(Console[IO].println("trace this operation"))
      _ <- Trace[IO].span("span2", SpanKind.Client)(Console[IO].println("send some request"))
      _ <- Trace[IO].span("span3", SpanKind.Client)(
        Trace[IO].putAll("attribute1" -> "test", "attribute2" -> 200) >>
          Trace[IO].setStatus(SpanStatus.Cancelled)
      )
    } yield {
      ()
    }
  }

}
