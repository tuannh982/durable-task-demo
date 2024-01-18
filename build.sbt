organization := "io.github.tuannh982"
scalaVersion := "2.12.17"
crossScalaVersions := Seq("2.12.17", "2.13.12", "3.3.1")
versionScheme := Some("early-semver")

lazy val root = project
  .in(file("."))
  .aggregate()
  .settings(
    name := "durable-task-demo",
    publish / skip := true
  )

libraryDependencies += "io.circe" %% "circe-parser"  % "0.14.6"
libraryDependencies += "io.circe" %% "circe-generic" % "0.14.6"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.15" % "test",
  "org.scalamock" %% "scalamock" % "5.2.0"  % "test"
)
