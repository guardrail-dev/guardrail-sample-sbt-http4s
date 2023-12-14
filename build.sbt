val Scala213 = "2.13.12"

ThisBuild / crossScalaVersions := Seq("2.12.13", Scala213)
ThisBuild / scalaVersion := crossScalaVersions.value.last

ThisBuild / githubWorkflowArtifactUpload := false

val Scala213Cond = s"matrix.scala == '$Scala213'"

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("test")),
)

val http4sV = "0.22.13"
val circeV = "0.13.0"
val logbackClassicV = "1.2.3"

val munitCatsEffectV = "0.12.0"

val kindProjectorV = "0.13.2"
val betterMonadicForV = "0.3.1"

// Projects
lazy val `guardrail-sample-http4s` = project.in(file("."))
  .settings(commonSettings)
  .settings(
    name := "guardrail-sample-http4s",


    // Where all the magic lives
    // Translates the openapi documentation into code generation targets
    // after `sbt compile` files will be populated in
    // `/src/target/scala-2.13/src_managed/main/example`
    // with folder for server and client which hold their respective generated code.
    Compile / guardrailTasks := List(
      ScalaServer(file("server.yaml"), pkg="example.server", framework="http4s-v0.22", tagsBehaviour=tagsAsPackage),
      ScalaClient(file("server.yaml"), pkg="example.client", framework="http4s-v0.22", tagsBehaviour=tagsAsPackage),
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

Compile / doc / scalacOptions ++= Seq(
    "-groups",
    "-sourcepath", (LocalRootProject / baseDirectory).value.getAbsolutePath,
    "-doc-source-url", "https://github.com/guardrail-dev/guardrail-sample-http4s/blob/v" + version.value + "€{FILE_PATH}.scala"
)

// General Settings
inThisBuild(List(
  organization := "com.example",
  developers := List(
    Developer("ChristopherDavenport", "Christopher Davenport", "chris@christopherdavenport.tech", url("https://github.com/ChristopherDavenport"))
  ),

  homepage := Some(url("https://github.com/guardrail-dev/guardrail-sample-http4s")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),

  pomIncludeRepository := { _ => false},
))
