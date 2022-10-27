package trace.headers.b3

import org.http4s.util.{Renderable, Writer}
import org.http4s.{Header, ParseResult}
import org.typelevel.ci._

object `X-B3-TraceId` {
  private val headerName = getClass.getSimpleName

  def apply(traceId: String): `X-B3-TraceId` = new `X-B3-TraceId`(traceId)

  implicit val headerInstance: Header[`X-B3-TraceId`, Header.Single] =
    Header.createRendered(
      ci"$headerName",
      (h: `X-B3-TraceId`) =>
        new Renderable {
          def render(writer: Writer): writer.type = {
            writer << h.traceId
          }
        },
      parse
    )

  private def parse(headerValue: String): ParseResult[`X-B3-TraceId`] = Right(
    `X-B3-TraceId`(headerValue)
  )

}

case class `X-B3-TraceId`(traceId: String)
