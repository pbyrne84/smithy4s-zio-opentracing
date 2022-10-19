namespace smithy4s.hello

use smithy4s.api#simpleRestJson


@simpleRestJson
service HelloWorldService {
    version: "1.0.0",
    operations: [Hello]
}

@documentation("This *is* documentation about the shape.")
@http(method: "POST", uri: "/{name}", code: 200)
operation Hello {
    input: Person,
    output: Greeting
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

