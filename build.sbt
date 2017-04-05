name := "nlp"

version := "0.4"

scalaVersion := "2.11.9"

dependencyUpdatesExclusions := moduleFilter(organization = "org.scala-lang")

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.10"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.4.17"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.4.17"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.17"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.17"

libraryDependencies += "jline" % "jline" % "2.14.3"

libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"

libraryDependencies += "com.softwaremill.macwire" %% "util" % "2.3.0"

libraryDependencies += "com.softwaremill.macwire" %% "proxy" % "2.3.0"

val utils =
  Project(id = "utils", base = file("utils")).dependsOn()

val wordmetrix =
  Project(id = "wordmetrix", base = file("wordmetrix")).dependsOn(utils)

val akkaUtils =
  Project(id = "akka-utils", base = file("akka-utils")).dependsOn(utils, wordmetrix)

val index =
  Project(id = "index", base = file("index")).dependsOn(wordmetrix)

val indexStream =
  Project(id = "index-stream", base = file("index-stream")).dependsOn(wordmetrix, akkaUtils)

val repl =
  Project(id = "repl", base = file("repl")).dependsOn(wordmetrix, akkaUtils, utils)

val ml =
  Project(id = "ml", base = file("ml")).dependsOn(utils)

val www =
  Project(id = "web", base = file("web")).dependsOn(wordmetrix, akkaUtils, utils)

lazy val root = Project(
  id = "nlp",
  base = file("."),
  settings = Defaults.coreDefaultSettings //Project.defaultSettings
).dependsOn().aggregate(wordmetrix, index, indexStream, repl, akkaUtils, www, ml, utils)

//enablePlugins(ScalaKataPlugin)