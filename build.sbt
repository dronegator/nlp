name := "nlp"

version := "0.4"

scalaVersion := "2.11.9"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.10"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.4.17"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.4.17"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.17"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.17"

//libraryDependencies += "com.typesafe" % "config" % "1.3.1"
//
//libraryDependencies += "com.github.kxbmap" %% "configs" % "0.4.4"

//libraryDependencies += "jline" % "jline" % "2.14.2"

libraryDependencies += "jline" % "jline" % "2.14.3"

libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"

libraryDependencies += "com.softwaremill.macwire" %% "util" % "2.3.0"

libraryDependencies += "com.softwaremill.macwire" %% "proxy" % "2.3.0"



//enablePlugins(ScalaKataPlugin)