name := "Akka Playground"

version := "0.1.0"

scalaVersion := "2.11.2"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5",
  "com.typesafe.akka" %% "akka-remote" % "2.3.5",
  "com.typesafe.akka" %% "akka-agent" % "2.3.5",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.5" % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)
