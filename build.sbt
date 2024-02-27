ThisBuild / crossScalaVersions := Seq("2.12.18", "2.13.13")
ThisBuild / scalaVersion := crossScalaVersions.value.last

ThisBuild / githubWorkflowArtifactUpload := false

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(List("test")),
)

ThisBuild / githubWorkflowPublishTargetBranches := Seq.empty

ThisBuild / scalacOptions ++= Seq(
  "-deprecation"
)

val http4sV = "0.23.16"
val circeV = "0.14.6"
val logbackClassicV = "1.4.4"

val munitCatsEffectV = "1.0.7"

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
      ScalaServer(file("server.yaml"), pkg="example.server", framework="http4s", tagsBehaviour=tagsAsPackage),
      ScalaClient(file("server.yaml"), pkg="example.client", framework="http4s", tagsBehaviour=tagsAsPackage),
    )

  )

// General Settings
lazy val commonSettings = Seq(
  testFrameworks += new TestFramework("munit.Framework"),
  libraryDependencies ++= {
    Seq(
      compilerPlugin("org.typelevel" % "kind-projector" % kindProjectorV cross CrossVersion.full),
      compilerPlugin("com.olegpy" %% "better-monadic-for" % betterMonadicForV),
    )
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

    "org.typelevel"               %% "munit-cats-effect-3"        % munitCatsEffectV         % Test,
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
