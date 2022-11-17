package zioexample.tracing

import zio.Scope
import zio.test._

object B3Spec extends ZIOSpecDefault {
  private val baseHeaders: List[(String, String)] = List(
    "headerName1" -> "headerValue1",
    "headerName2" -> "headerValue2"
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("B3 defaultSampledHeader should")(
      test("add default sampled header if one does not exist") {
        val headersSetToDefault = List(true, false)
          .map(enabled => enabled -> B3.defaultSampledHeader(headers = baseHeaders, enabled))

        assertTrue(
          headersSetToDefault == List(
            true -> (baseHeaders ++ List(B3.header.sampled -> "1")),
            false -> (baseHeaders ++ List(B3.header.sampled -> "0"))
          )
        )
      },
      test("leave the sampled header if it does exist") {
        assertTrue(
          B3.defaultSampledHeader(headers =
            baseHeaders ++ List(B3.header.sampled -> "0")
          ) == baseHeaders ++ List(
            B3.header.sampled -> "0"
          )
        )
      },
      test("replace invalid header values") {
        val actual: Seq[(String, List[(String, String)])] = List("a", "2", "+").map {
          invalidValue =>
            val invalidHeader = (B3.header.sampled -> invalidValue)

            invalidValue -> B3.defaultSampledHeader(baseHeaders ++ List(invalidHeader))
        }

        val defaultHeader = List(B3.header.sampled -> "1")
        val expected: Seq[(String, List[(String, String)])] = List(
          "a" -> (baseHeaders ++ defaultHeader),
          "2" -> (baseHeaders ++ defaultHeader),
          "+" -> (baseHeaders ++ defaultHeader)
        )

        assertTrue(actual == expected)

      }
    )
}
