package zioexample.tracing

import zio.Scope
import zio.test._

object B3Spec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("B3 defaultSampledHeader should")(
      test("add default sampled header if one does not exist") {
        val headersSetToDefault = List(true, false)
          .map(enabled => B3.defaultSampledHeader(headers = List.empty, enabled))

        assertTrue(
          headersSetToDefault == List(
            List(B3.header.sampled -> "1"),
            List(B3.header.sampled -> "0")
          )
        )
      },
      test("leave the sampled header if it does exist") {
        assertTrue(
          B3.defaultSampledHeader(headers = List(B3.header.sampled -> "0")) == List(
            B3.header.sampled -> "0"
          )
        )
      },
      test("replace invalid header values") {
        val actual: Seq[(String, List[(String, String)])] = List("a", "2", "+").map {
          invalidValue =>
            val invalidHeader = (B3.header.sampled -> invalidValue)

            invalidValue -> B3.defaultSampledHeader(List(invalidHeader))
        }

        val defaultHeader = List(B3.header.sampled -> "1")
        val expected: Seq[(String, List[(String, String)])] = List(
          "a" -> defaultHeader,
          "2" -> defaultHeader,
          "+" -> defaultHeader
        )

        assertTrue(actual == expected)

      }
    )
}
