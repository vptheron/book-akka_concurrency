name := "Akka Playground"

version := "0.1.0"

scalaVersion := "2.11.2"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.5"
)
