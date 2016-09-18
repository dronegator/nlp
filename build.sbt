name := "nlp"

version := "0.4"

scalaVersion := "2.11.7"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies +=  "org.scalacheck" %% "scalacheck" % "1.10.1" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.4.9"

libraryDependencies +=  "com.typesafe.akka" %% "akka-slf4j" % "2.4.9-RC2"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.4.9-RC2"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.9-RC2"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.9-RC2"

//libraryDependencies += "jline" % "jline" % "2.14.2"

libraryDependencies += "jline" % "jline" % "2.11"

libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.2.3" % "provided"

libraryDependencies += "com.softwaremill.macwire" %% "util" % "2.2.3"

libraryDependencies += "com.softwaremill.macwire" %% "proxy" % "2.2.3"

//enablePlugins(ScalaKataPlugin)