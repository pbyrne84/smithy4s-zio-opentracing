namespace smithy4s.hello

use smithy4s.api#simpleRestJson


@simpleRestJson
service HelloWorldService {
    version: "1.0.0",
    operations: [Hello]
}

@http(method: "POST", uri: "/{name}", code: 200)
operation Hello {
    input: Person,
    output: Greeting
}

structure Person {
    @httpLabel
    @required
    @pattern("^[A-Za-z0-9 ]+$\\{'$'}")
    name: String,

    @httpQuery("town")
    town: String
}

structure Greeting {
    @required
    message: String
}