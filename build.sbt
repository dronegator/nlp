import Dependencies._
import sbt.Keys._

name := "nlp"

version := "0.4"

scalaVersion := "2.11.9"

dependencyUpdatesExclusions := moduleFilter(organization = ScalaArtifacts.Organization)

libraryDependencies ++= defaultDependencies

libraryDependencies ++= akkaDependencies

lazy val utils =
  Project(id = "utils", base = file("utils"))
    .settings(libraryDependencies ++= defaultDependencies)
    .dependsOn()

lazy val wordmetrix =
  Project(id = "wordmetrix", base = file("wordmetrix"))
    .settings(libraryDependencies ++= defaultDependencies)
    .settings(libraryDependencies ++= serviceDependencies)
    .dependsOn(utils)

lazy val akkaUtils =
  Project(id = "akka-utils", base = file("akka-utils"))
    .settings(libraryDependencies ++= akkaDependencies)
    .dependsOn(utils, wordmetrix)

lazy val index =
  Project(id = "index", base = file("index")).dependsOn(wordmetrix)

lazy val indexStream =
  Project(id = "index-stream", base = file("index-stream"))
    .settings(libraryDependencies ++= akkaDependencies)
    .dependsOn(wordmetrix, akkaUtils)

lazy val repl =
  Project(id = "repl", base = file("repl"))
    .dependsOn(wordmetrix, akkaUtils, utils, ml)

lazy val ml =
  Project(id = "ml", base = file("ml"))
    .dependsOn(wordmetrix, akkaUtils, utils)

lazy val www =
  Project(id = "web", base = file("web"))
    .dependsOn(wordmetrix, akkaUtils, utils)

lazy val root = Project(
  id = "nlp",
  base = file("."),
  settings = Defaults.coreDefaultSettings //Project.defaultSettings
).dependsOn().aggregate(wordmetrix, index, indexStream, repl, akkaUtils, www, ml, utils)
