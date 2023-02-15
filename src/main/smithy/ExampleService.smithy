$version: "2.0"
namespace smithy4s.hello

use smithy4s.api#simpleRestJson


@simpleRestJson
service HelloWorldService {
    version: "1.0.0",
    operations: [Hello, UpdatePerson]
    errors: [GenericServerError, GenericBadRequestError, GenericUnprocessableEntityError]
}

@mixin
structure RequestTracing {
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
structure GenericServerError with [RequestTracing] {
    @required
    message: String
}

@error("client")
@httpError(400)
structure GenericBadRequestError with [RequestTracing] {
    @required
    message: String

    @required
    uri: String
}


@error("client")
@httpError(422)
structure GenericUnprocessableEntityError with [RequestTracing] {
    @required
    message: String
}


@documentation("Gets a person by their name")
@http(method: "GET", uri: "/person/{name}", code: 200)
operation Hello {
    input: Person,
    output: Greeting,
    errors: [GenericServerError, GenericBadRequestError, GenericUnprocessableEntityError]
}


structure Person {
    @httpLabel
    @required
    @pattern("^[A-Z]+")
    name: String

    @httpQuery("town")
    town: String
}

structure Greeting with [RequestTracing] {
    @required
    message: String
}


@documentation("This *is* documentation about the shape.")
@http(method: "POST", uri: "/person", code: 200)
operation UpdatePerson {
    input: PersonUpdate,
    output: PersonResponse,
    errors: [GenericServerError, GenericBadRequestError, GenericUnprocessableEntityError]
}


structure PersonUpdate {
    // httpPayload makes the request just need a PersonUpdatePayload, and not have a field called data
    // with PersonUpdatePayload in it
    @httpPayload
    @required
    data: PersonUpdatePayload
}

structure PersonResponse with [RequestTracing] {
    @httpPayload
    @required
    data: PersonUpdatePayload
}


structure PersonUpdatePayload {
    @required
    name: String
    newName: String
    newTown: String
}


