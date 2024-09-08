lazy val scala3_5_0 = "3.5.0"
lazy val scala2_13_14 = "2.13.14"
lazy val scala2_12_19 = "2.12.19"
lazy val supportedScalaVersions = Vector(scala3_5_0, scala2_13_14, scala2_12_19)
ThisBuild / version := "0.1.1"
ThisBuild / organization := "gg.sina"
ThisBuild / scalaVersion := scala3_5_0
ThisBuild / crossScalaVersions := supportedScalaVersions
ThisBuild / versionScheme := Some("early-semver")

val commonSettings = Seq(
  githubTokenSource := TokenSource.GitConfig("tokens.sbt"),
  githubOwner := "sinaghaffari",
  githubRepository := "monadic-simplifier",
)

lazy val root = (project in file("."))
  .settings(
    commonSettings,
    crossScalaVersions := Nil,
    publish / skip := true
  )
  .aggregate(core, json)

lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    name := "monadic-simplifier",
  )

lazy val json = (project in file("play-json"))
  .settings(
    commonSettings,
    name := "monadic-simplifier-play-json",
    libraryDependencies += "org.playframework" %% "play-json" % "3.1.0-M1"
  )
  .dependsOn(core)