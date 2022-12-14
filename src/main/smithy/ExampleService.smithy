$version: "2.0"
namespace smithy4s.hello

use smithy4s.api#simpleRestJson


@simpleRestJson
service HelloWorldService {
    version: "1.0.0",
    operations: [Hello]
    errors: [GenericServerError,GenericBadRequestError,GenericUnprocessableEntityError]
}

@mixin
structure OutgoingRequestTracing {
    @required
    @httpHeader("X-B3-TraceId")
    traceId: String

    @required
    @httpHeader("X-B3-SpanId")
    spanId: String

    @required
    @httpHeader("X-B3-Sampled")
    sampled: String
}

@error("server")
@httpError(500)
structure GenericServerError with [OutgoingRequestTracing] {
    @required
    message: String
}

@error("client")
@httpError(400)
structure GenericBadRequestError with [OutgoingRequestTracing]  {
    @required
    message: String
}

@error("client")
@httpError(422)
structure GenericUnprocessableEntityError with [OutgoingRequestTracing] {
    @required
    message: String
}


@documentation("This *is* documentation about the shape.")
@http(method: "GET", uri: "/{name}", code: 200)
operation Hello {
    input: Person,
    output: Greeting,
    errors: [GenericServerError,GenericBadRequestError,GenericUnprocessableEntityError]
}


@mixin
structure IncomingRequestTracing {
    @required
    @httpHeader("X-B3-TraceId")
    traceId: String

    @required
    @httpHeader("X-B3-SpanId")
    spanId: String

    @required
    @httpHeader("X-B3-Sampled")
    sampled: String
}


structure Person {
    @httpLabel
    @required
    @pattern("^[A-Z]+")
    name: String,

    @httpQuery("town")
    town: String
}

structure Greeting with [IncomingRequestTracing]{
    @required
    message: String
}

