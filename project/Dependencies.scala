import sbt._

object Dependencies {
  val circeVersion = "0.8.0"

  // Java dependencies
  lazy val httpClient = "org.apache.httpcomponents" % "httpclient" % "4.5.3"
  lazy val jackson = "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.0"
  lazy val junit = "junit" % "junit" % "4.12"
  lazy val mockito = "org.mockito" % "mockito-core" % "2.8.47"

  // Scala dependencies
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.0.1"
  lazy val akkaHttp = "com.typesafe.akka" %% "akka-http" % "10.0.9"
  lazy val circe = Seq(
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion)
}
