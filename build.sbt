val tapirVersion = "1.9.4"

lazy val rootProject = (project in file(".")).settings(
  Seq(
    name := "dawn-patrol-api",
    version := "0.1.0-SNAPSHOT",
    organization := "xyz.didx",
    scalaVersion := "3.3.1",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.http4s" %% "http4s-ember-server" % "0.23.24",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "ch.qos.logback" % "logback-classic" % "1.4.14",
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
      "org.scalatest" %% "scalatest" % "3.2.17" % Test,
      "com.softwaremill.sttp.client3" %% "circe" % "3.9.1" % Test
    )
  )
)
