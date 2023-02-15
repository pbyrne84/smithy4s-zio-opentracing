package zioexample.service

import io.opentelemetry.sdk.trace.SdkTracerProvider
import org.http4s.{Headers, Request}
import zio.test._
import zio.{Cause, LogLevel, LogSpan, Scope, Task, ZIO, ZLayer}
import zioexample.logging.SL4JTestLogger
import zioexample.tracing.B3

import scala.collection.mutable.ListBuffer

case class TracedContext(
    logMessage: String,
    spanName: String,
    traceId: String,
    parentSpanIdMatchesPreviousSpanId: Option[Boolean]
)

object ZioExampleServiceSpec extends ZIOSpecDefault {
  private val traceId = "01115d8eb7e102b505085969c4aca859"
  private val baseHeaders = List(
    B3.header.traceId -> traceId,
    B3.header.spanId -> "40ce80b7c43f2884"
  )

  private val tracingLayer = ZLayer {
    ZIO.succeed(SdkTracerProvider.builder().build().tracerBuilder(this.getClass.toString).build())
  }

  final case class LogEntry(
      span: List[String],
      level: LogLevel,
      annotations: Map[String, String],
      message: String,
      cause: Cause[Any],
      spans: List[LogSpan]
  )

  private val firstEntry: TracedContext = TracedContext(
    logMessage = "chupacabra",
    spanName = "hello-route",
    traceId = traceId,
    parentSpanIdMatchesPreviousSpanId = None
  )

  // Sampling enables parent span relationships for graphing etc.
  private val expectedSampledResult = List(
    // Cannot tell if the first parentSpanId matches the previous as the previous is unknown.
    firstEntry,
    TracedContext(
      logMessage = "demogorgon",
      spanName = "hello-route-child-span",
      traceId = traceId,
      parentSpanIdMatchesPreviousSpanId = Some(true)
    )
  )

  //
  private val expectedNonSampledResult = List(
    // Cannot tell if the first parentSpanId matches the previous as the previous is unknown.
    firstEntry,
    TracedContext(
      logMessage = "demogorgon",
      spanName = "hello-route-child-span",
      traceId = traceId,
      parentSpanIdMatchesPreviousSpanId = Some(
        false
      ) // the only thing different we can actually test as things are generated underneath
    )
  )

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ZioExampleService")(
      test(
        "should send span tracing through the system when sampling is enabled via header and defaultToAlwaysSample is disabled"
      ) {
        val headers = baseHeaders :+ (B3.header.sampled -> "1")
        val request = createRequest(headers)

        // mutable ick but the fibre handling is local underneath the call
        val contextLogEntries = ListBuffer.empty[Map[String, String]]

        // Extra level of indentation so we can scope the testlogger.createLayer
        {
          for {
            greeting <- ZioExampleService.hello(
              request,
              "name",
              None,
              defaultToAlwaysSample = false
            )
          } yield assertTrue(
            convertContextLogEntries(contextLogEntries.toList) == expectedSampledResult
          )
        }.provide(
          zio.telemetry.opentelemetry.Tracing.live,
          tracingLayer,
          SL4JTestLogger.createLayer(contextLogEntries) // side effect on each log call
        )
      },
      test(
        "should send span tracing through the system when sampling is not enabled via header and defaultToAlwaysSample is enabled"
      ) {
        val headers = baseHeaders
        val request = createRequest(headers)

        // mutable ick but the fibre handling is local underneath the call
        val contextLogEntries = ListBuffer.empty[Map[String, String]]

        // Extra level of indentation so we can scope the testlogger.createLayer
        {
          for {
            greeting <- ZioExampleService.hello(request, "name", None, defaultToAlwaysSample = true)
          } yield {
            assertTrue(
              convertContextLogEntries(contextLogEntries.toList) == expectedSampledResult,
              greeting.traceId == traceId,
              greeting.spanId.matches("([a-f\\d]{16})"),
              greeting.message == "Hello Hello name!!"
            )
          }
        }.provide(
          zio.telemetry.opentelemetry.Tracing.live,
          tracingLayer,
          SL4JTestLogger.createLayer(contextLogEntries) // side effect on each log call
        )
      },
      test(
        "should not send span tracing through the system when sampling is not enabled via header and defaultToAlwaysSample is not enabled"
      ) {
        val headers = baseHeaders
        val request = createRequest(headers)

        // mutable ick but the fibre handling is local underneath the call
        val contextLogEntries = ListBuffer.empty[Map[String, String]]

        // Extra level of indentation so we can scope the testlogger.createLayer
        {
          for {
            greeting <- ZioExampleService.hello(
              request,
              "name",
              None,
              defaultToAlwaysSample = false
            )
          } yield assertTrue(
            convertContextLogEntries(contextLogEntries.toList) == expectedNonSampledResult
          )
        }.provide(
          zio.telemetry.opentelemetry.Tracing.live,
          tracingLayer,
          SL4JTestLogger.createLayer(contextLogEntries) // side effect on each log call
        )
      }
    )
  }

  private def createRequest(headers: List[(String, String)]): Request[Task] = {
    val requestHeaders = headers.foldLeft(Headers.empty) { case (currentHeaders, header) =>
      currentHeaders.put(header)
    }

    Request(headers = requestHeaders)
  }

  private def convertContextLogEntries(entries: List[Map[String, String]]): List[TracedContext] = {
    entries
      .foldLeft((List.empty[TracedContext], Option.empty[String])) {
        case ((contexts, maybePreviousSpanId), current) =>
          val logMessage = current.getOrElse("message", "")
          val currentSpanName = current.getOrElse("span_name", "")
          val traceId = current.getOrElse("trace_id", "")
          val currentSpanId = current.getOrElse("span_id", "")
          val currentParentSpanId = current.getOrElse("parent_span_id", "")

          val maybParentSpanMatchesPreviousSpanId =
            maybePreviousSpanId.map(_ == currentParentSpanId)

          val amendedList = contexts :+ TracedContext(
            logMessage,
            currentSpanName,
            traceId,
            maybParentSpanMatchesPreviousSpanId
          )

          (amendedList, Some(currentSpanId))
      }
      ._1
  }

}
