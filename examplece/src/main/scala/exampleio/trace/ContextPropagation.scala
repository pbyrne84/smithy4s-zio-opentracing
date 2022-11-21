package exampleio.trace

import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.{TextMapGetter, TextMapPropagator, TextMapSetter}

import java.util
import scala.jdk.CollectionConverters.SeqHasAsJava

object ContextPropagation {

  def extractContext[C](
      propagator: TextMapPropagator,
      carrier: C,
      getter: TextMapGetter[C]
  ): IO[Context] =
    IO(propagator.extract(Context.root(), carrier, getter))

  def injectContext[C](
      context: Context,
      propagator: TextMapPropagator,
      carrier: C,
      setter: TextMapSetter[C]
  ): IO[Unit] =
    IO(propagator.inject(context, carrier, setter))

}

class HeaderTextMapGetter extends TextMapGetter[List[(String, String)]] with StrictLogging {
  override def keys(carrier: List[(String, String)]): util.List[String] = {
    logger.info(s"getting keys $carrier for tracing")
    carrier.map(_._1.toString).asJava
  }

  // This will be called B3PropagatorExtractorMultipleHeaders using the values passed in
  // the header list in test.spanFrom.
  // At the moment sampled header need to be there which is a bit annoying as you can hack adding the
  // header all the time in the route but that is pants. There should be no faith in any calling entity
  // to be correct.
  //
  // So if the 3 headers are not there or have incorrect values then we get varied results from traced ids that are invalid
  // (00000000000000000000000000000000 and 0000000000000000) or are not set at all
  //
  // Header matching should always be case insensitive as well
  override def get(carrier: List[(String, String)], key: String): String = {
    logger.info(s"get key from $carrier -  $key")
    carrier.find(_._1.toString.toLowerCase == key.toLowerCase).map(_._2.toString).orNull
  }
}
