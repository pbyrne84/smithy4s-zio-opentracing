package zioexample

import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.sdk.trace.SdkTracerProvider
import zio.{ZIO, ZLayer}

object TracingLayers {
  // builder builder builder to get around inaccessibility of things
  private val tracer: Tracer =
    SdkTracerProvider.builder().build().tracerBuilder(this.getClass.toString).build()

  val tracerLayer = ZLayer(ZIO.succeed(tracer))
}
