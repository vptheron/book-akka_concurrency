import sbt._
import Keys._
import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm

object AkkaBookBuild extends Build {

  val ScalaVersion = "2.11.2"

  object Dependencies {
    val AkkaVersion = "2.3.5"
    val ScalaTestVersion = "2.2.1"

    val avionics = Seq(
      "com.typesafe.akka" %% "akka-actor" % AkkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % AkkaVersion,
      "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
      "com.typesafe.akka" %% "akka-agent" % AkkaVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion
    )
  }

  lazy val multiJvmSettings = SbtMultiJvm.multiJvmSettings ++
    Seq(
      // make sure that MultiJvm test are compiled by the default test compilation
      compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
      // disable parallel tests
      parallelExecution in Test := false,
      // make sure that MultiJvm tests are executed by the default test target
      executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
        case (testResults, multiNodeResults) =>
          val overall =
            if (testResults.overall.id < multiNodeResults.overall.id)
              multiNodeResults.overall
            else
              testResults.overall
          Tests.Output(overall,
            testResults.events ++ multiNodeResults.events,
            testResults.summaries ++ multiNodeResults.summaries)
      }
    )

  lazy val buildSettings = Project.defaultSettings ++
    multiJvmSettings ++ Seq(
    organization := "zzz.akka",
    name := "akka-book",
    version := "0.1",
    scalaVersion := ScalaVersion,
    autoCompilerPlugins := true,
    resolvers ++= Seq(
      "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype" at "https://oss.sonatype.org/content/groups/public/"
    ),
    libraryDependencies ++= Dependencies.avionics
  )

  lazy val avionics = Project(
    id = "avionics",
    base = file("."),
    settings = buildSettings
  ) configs MultiJvm

}