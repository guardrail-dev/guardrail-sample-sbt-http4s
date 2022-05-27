// addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.16") // - Makes it very angry
addSbtPlugin("ch.epfl.lamp" % "sbt-dotty" % "0.5.1")
addSbtPlugin("com.codecommit" % "sbt-github-actions" % "0.9.5")

// Add the plugin to the project
addSbtPlugin("dev.guardrail" % "sbt-guardrail" % "0.71.0")

libraryDependencies ++= Seq(
  "dev.guardrail" %% "guardrail-core" % "0.71.0",
  "dev.guardrail" %% "guardrail-scala-support" % "0.71.1",
  "dev.guardrail" %% "guardrail-scala-http4s" % "0.72.0",
  "dev.guardrail" %% "guardrail" % "0.71.0"
)
