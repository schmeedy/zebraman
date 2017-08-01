import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.2",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",

    libraryDependencies ++= Seq(
      jackson,
      httpClient,
      akkaHttp,
      scalaTest % Test,
      junit % Test,
      mockito % Test
    ) ++ circe,

    mainClass in assembly := Some("com.github.schmeedy.zonky.Main"),
    assemblyJarName in assembly := "zebraman.jar"
  )
