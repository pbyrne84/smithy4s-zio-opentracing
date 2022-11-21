package exampleio.trace

import cats.effect.testing.scalatest.AsyncIOSpec
import io.opentelemetry.extension.trace.propagation.B3Propagator

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec

class ContextPropagationSpec extends AsyncWordSpec with AsyncIOSpec with Matchers {

  "ContextPropagation.extractContext" should {
    "initialise if correct headers are passed " in {
      val traceId = "01115d8eb7e102b505085969c4aca859"
      val spanId = "40ce80b7c43f2884"

      ContextPropagation
        .extractContext(
          B3Propagator.injectingMultiHeaders(),
          carrier = List(
            trace.B3.header.traceId -> traceId,
            trace.B3.header.spanId -> spanId,
            trace.B3.header.sampled -> "1"
          ),
          new HeaderTextMapGetter
        )
        .asserting { context =>
          // which span is the right span as there are so many spans in this project
          val maybeSpanContext = Option(io.opentelemetry.api.trace.Span.fromContextOrNull(context))
            .map(_.getSpanContext)

          maybeSpanContext.map(_.getTraceId) shouldBe Some(traceId)
          maybeSpanContext.map(_.getSpanId) shouldBe Some(spanId)

        }
    }
  }

}
