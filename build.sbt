scalaVersion := "2.13.8"
version := "0.1"
name := "smithy4s-zio-opentracing"

val zioConfigVersion = "3.0.7"
val zioVersion = "2.0.9"
val zioLoggingVersion = "2.1.1"
val natchezExtrasVersion = "6.2.4"
val natchezVersion = "0.1.5"

scalacOptions in GlobalScope ++= Seq(
  "-Xfatal-warnings",
  "-feature",
  "-language:higherKinds",
  "-deprecation"
)

libraryDependencies ++= List(
  "ch.qos.logback" % "logback-classic" % "1.4.5",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.2",
  "org.slf4j" % "jul-to-slf4j" % "2.0.5",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "io.jaegertracing" % "jaeger-core" % "1.8.1",
  "io.janstenpickle" %% "trace4cats-avro-exporter" % "0.14.0"
)

val example = project
  .in(file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s" % smithy4sVersion.value,
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s-swagger" % smithy4sVersion.value,
      "org.http4s" %% "http4s-ember-server" % "0.23.16"
    ),
    addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full))
  )
  .settings(scalaVersion := "2.13.8")

Test / parallelExecution := false

Test / test := (Test / test)
  .dependsOn(Compile / scalafmtCheck)
  .dependsOn(Test / scalafmtCheck)
  .value

lazy val exampleIo = (project in file("exampleio"))
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= List(
      "io.janstenpickle" %% "trace4cats-core" % "0.14.0",
      "org.tpolecat" %% "natchez-log" % natchezVersion,
      "org.tpolecat" %% "natchez-http4s" % "0.3.2",
      "org.tpolecat" %% "natchez-jaeger" % natchezVersion,
      "io.janstenpickle" %% "trace4cats-avro-exporter" % "0.14.0",
      "com.ovoenergy" %% "natchez-extras-http4s-stable" % natchezExtrasVersion
    ),
    addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full))
  )
  .dependsOn(example)

val circeVersion = "0.14.4"
lazy val exampleZio = (project in file("examplezio"))
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= List(
      "dev.zio" %% "zio" % zioVersion,
      "io.d11" %% "zhttp" % "2.0.0-RC10",
      "org.http4s" %% "http4s-blaze-server" % "0.23.13",
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.opentelemetry" % "opentelemetry-extension-trace-propagators" % "1.23.0",
      "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % "1.17.0-alpha",
      "io.opentelemetry" % "opentelemetry-exporter-zipkin" % "1.23.0",
      "io.opentelemetry" % "opentelemetry-exporter-jaeger" % "1.23.0",
      "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
      "dev.zio" %% "zio-logging-slf4j-bridge" % zioLoggingVersion,
      "dev.zio" %% "zio-interop-cats" % "23.0.0.0",
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-config-refined" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-opentelemetry" % "2.0.3",
      "dev.zio" %% "zio-opentracing" % "2.0.3",
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test" % zioVersion % Test
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .dependsOn(example)

//not to be used in ci, intellij has got a bit bumpy in the format on save
val formatZioAndRunTests =
  taskKey[Unit](
    "format all the ZIO modules, do not use on CI as any changes will not be committed"
  )

formatZioAndRunTests := {
  (exampleZio / Test / test)
    .dependsOn(exampleZio / Compile / scalafmtAll)
    .dependsOn(exampleZio / Test / scalafmtAll)
    .dependsOn(Compile / scalafmtSbt)
}.value
