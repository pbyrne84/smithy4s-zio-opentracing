package zioexample.logging

import zio._
import zio.logging.LogFormat

import scala.collection.mutable.ListBuffer

final case class MDCLogEntry(
    span: List[String],
    level: LogLevel,
    annotations: Map[String, String],
    message: String,
    cause: Cause[Any],
    spans: List[LogSpan],
    mdcKeys: Map[String, String]
)

/** Logger that appends the log context as we want to test the tracing across the fibres
  * @param contextLogEntries
  */
object SL4JTestLogger {

  val logFormatDefault: LogFormat = LogFormat.allAnnotations + LogFormat.line + LogFormat.cause

  def createLayer(
      contextLogEntries: ListBuffer[Map[String, String]]
  ): ZLayer[Any, Nothing, Unit] = {

    val testFormat: LogFormat =
      LogFormat.make { (builder, a, b, c, messageCall, d, fibrerRefs, e, annotations) =>
        import zio.logging._

        fibrerRefs.get(logContext).map { logContext =>

          val logContextWithMessage = Map("message" -> messageCall()) ++ logContext.asMap

          contextLogEntries.append(logContextWithMessage)
        }

      }

    Runtime.addLogger(testFormat.toLogger)
  }

}
