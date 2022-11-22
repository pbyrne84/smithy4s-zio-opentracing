scalaVersion := "2.13.8"
version := "0.1"
name := "smithy4s-zio-opentracing"

val zioConfigVersion = "3.0.2"
val zioVersion = "2.0.0"
def zioLoggingVersion = "2.1.1"
//val zioLoggingVersion = "2.1.0+3-a733f542+20220908-1603-SNAPSHOT"
val natchezExtrasVersion = "6.2.4"
val natchezVersion = "0.1.5"

scalacOptions in GlobalScope ++= Seq(
  "-Xfatal-warnings",
  "-feature",
  "-language:higherKinds",
  "-deprecation"
)

libraryDependencies ++= List(
  "ch.qos.logback" % "logback-classic" % "1.4.0",
  "net.logstash.logback" % "logstash-logback-encoder" % "7.2",
  "org.slf4j" % "jul-to-slf4j" % "1.7.36",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "io.jaegertracing" % "jaeger-core" % "1.6.0",
  "io.janstenpickle" %% "trace4cats-avro-exporter" % "0.14.0"
)

val example = project
  .in(file("."))
  .enablePlugins(Smithy4sCodegenPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "io.opentelemetry" % "opentelemetry-extension-trace-propagators" % "1.17.0",
      "io.opentelemetry" % "opentelemetry-sdk-extension-autoconfigure" % "1.17.0-alpha",
      "io.opentelemetry" % "opentelemetry-exporter-zipkin" % "1.17.0",
      "io.opentelemetry" % "opentelemetry-exporter-jaeger" % "1.17.0",
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

lazy val examplece = (project in file("examplece"))
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= List(
      "io.janstenpickle" %% "trace4cats-core" % "0.14.0",
      "org.tpolecat" %% "natchez-log" % natchezVersion,
      "org.tpolecat" %% "natchez-http4s" % "0.3.2",
      "org.tpolecat" %% "natchez-jaeger" % natchezVersion,
      "io.janstenpickle" %% "trace4cats-avro-exporter" % "0.14.0",
      "org.typelevel" %% "log4cats-slf4j" % "2.5.0",
      "com.ovoenergy" %% "natchez-extras-http4s-stable" % natchezExtrasVersion,
      "org.scalatest" %% "scalatest" % "3.2.14" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test
    ),
    addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full))
  )
  .dependsOn(example)

lazy val examplezio = (project in file("examplezio"))
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= List(
      "dev.zio" %% "zio" % zioVersion,
      "io.d11" %% "zhttp" % "2.0.0-RC10",
      "org.http4s" %% "http4s-blaze-server" % "0.23.12",
      "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
      "dev.zio" %% "zio-logging-slf4j-bridge" % zioLoggingVersion,
      "dev.zio" %% "zio-interop-cats" % "3.3.0",
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "dev.zio" %% "zio-config-refined" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-opentelemetry" % "2.0.1",
      "dev.zio" %% "zio-opentracing" % "2.0.1",
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test" % zioVersion % Test
    )
  )
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .dependsOn(example)
