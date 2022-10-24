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
}

@error("client")
@httpError(400)
structure GenericBadRequestError {
    message: String
}

@error("client")
@httpError(422)
structure GenericUnprocessableEntityError {
    message: String
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
}

structure Greeting {
    @required
    message: String
}

