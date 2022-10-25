namespace smithy4s.hello

use smithy4s.api#simpleRestJson


@simpleRestJson
service HelloWorldService {
    version: "1.0.0",
    operations: [Hello]
    errors: [GenericServerError,GenericBadRequestError,GenericUnprocessableEntityError]
}

@error("server")
@httpError(500)
structure GenericServerError {
    message: String

    @required
    @httpHeader("X-B3-TraceId")
    traceId: String

    @required
    @httpHeader("X-B3-ParentSpanId")
    parentSpanId: String

    @required
    @httpHeader("X-B3-SpanId")
    spanId: String

    @required
    @httpHeader("X-B3-Sampled")
    sampled: String
}

@error("client")
@httpError(400)
structure GenericBadRequestError {
    message: String

    @required
    @httpHeader("X-B3-TraceId")
    traceId: String

    @required
    @httpHeader("X-B3-ParentSpanId")
    parentSpanId: String

    @required
    @httpHeader("X-B3-SpanId")
    spanId: String

    @required
    @httpHeader("X-B3-Sampled")
    sampled: String
}

@error("client")
@httpError(422)
structure GenericUnprocessableEntityError {
    message: String

    @required
    @httpHeader("X-B3-TraceId")
    traceId: String

    @required
    @httpHeader("X-B3-ParentSpanId")
    parentSpanId: String

    @required
    @httpHeader("X-B3-SpanId")
    spanId: String

    @required
    @httpHeader("X-B3-Sampled")
    sampled: String
}



@documentation("This *is* documentation about the shape.")
@http(method: "GET", uri: "/{name}", code: 200)
operation Hello {
    input: Person,
    output: Greeting,
    errors: [GenericServerError,GenericBadRequestError,GenericUnprocessableEntityError]
}

structure Person {
    @httpLabel
    @required
    @pattern("^[A-Z]+")
    name: String,

    @httpQuery("town")
    town: String

    @httpHeader("X-B3-TraceId")
    traceId: String

    @httpHeader("X-B3-ParentSpanId")
    parentSpanId: String

    @httpHeader("X-B3-SpanId")
    spanId: String

    @httpHeader("X-B3-Sampled")
    sampled: String
}

structure Greeting {
    @required
    message: String

    @required
    @httpHeader("X-B3-TraceId")
    traceId: String

    @required
    @httpHeader("X-B3-ParentSpanId")
    parentSpanId: String

    @required
    @httpHeader("X-B3-SpanId")
    spanId: String

    @required
    @httpHeader("X-B3-Sampled")
    sampled: String
}

