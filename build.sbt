val Scala213 = "2.13.5"

ThisBuild / crossScalaVersions := Seq("2.12.13", Scala213)
ThisBuild / scalaVersion := crossScalaVersions.value.last

ThisBuild / githubWorkflowArtifactUpload := false

val Scala213Cond = s"matrix.scala == '$Scala213'"

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("test")),
)

val http4sV = "0.21.20"
val circeV = "0.13.0"
val logbackClassicV = "1.2.3"

val munitCatsEffectV = "0.12.0"

val kindProjectorV = "0.11.3"
val betterMonadicForV = "0.3.1"

// Projects
lazy val `guardrail-http4s-sample` = project.in(file("."))
  .settings(commonSettings)
  .settings(
    name := "guardrail-http4s-sample",
    guardrailTasks in Compile := List(
      ScalaServer(file("server.yaml"), pkg="example.server", framework="http4s"),
      ScalaClient(file("server.yaml"), pkg="example.client", framework="http4s")
    )
  )

// General Settings
lazy val commonSettings = Seq(
  testFrameworks += new TestFramework("munit.Framework"),
  libraryDependencies ++= {
    if (isDotty.value) Seq.empty
    else Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % kindProjectorV cross CrossVersion.full),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV),
    )
  },
  scalacOptions ++= {
    if (isDotty.value) Seq("-source:3.0-migration")
    else Seq()
  },
  Compile / doc / sources := {
    val old = (Compile / doc / sources).value
    if (isDotty.value)
      Seq()
    else
      old
  },

  libraryDependencies ++= Seq(

    "org.http4s"                  %% "http4s-dsl"                 % http4sV,
    "org.http4s"                  %% "http4s-ember-server"        % http4sV,
    "org.http4s"                  %% "http4s-ember-client"        % http4sV,
    "org.http4s"                  %% "http4s-circe"               % http4sV,

    "io.circe"                    %% "circe-core"                 % circeV,
    "io.circe"                    %% "circe-generic"              % circeV,
    "io.circe"                    %% "circe-parser"               % circeV,

    "ch.qos.logback"              % "logback-classic"             % logbackClassicV,

    "org.typelevel"               %% "munit-cats-effect-2"        % munitCatsEffectV         % Test,
  )
)

// General Settings
inThisBuild(List(
  organization := "io.chrisdavenport",
  developers := List(
    Developer("ChristopherDavenport", "Christopher Davenport", "chris@christopherdavenport.tech", url("https://github.com/ChristopherDavenport"))
  ),

  homepage := Some(url("https://github.com/ChristopherDavenport/guardrail-http4s-sample")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),

  pomIncludeRepository := { _ => false},
  scalacOptions in (Compile, doc) ++= Seq(
      "-groups",
      "-sourcepath", (baseDirectory in LocalRootProject).value.getAbsolutePath,
      "-doc-source-url", "https://github.com/ChristopherDavenport/guardrail-http4s-sample/blob/v" + version.value + "€{FILE_PATH}.scala"
  )
))
