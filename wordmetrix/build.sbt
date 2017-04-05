name := "wordmetrix"

version := "0.4"

scalaVersion := "2.11.9"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.10"

libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"

libraryDependencies += "com.softwaremill.macwire" %% "util" % "2.3.0"

libraryDependencies += "com.softwaremill.macwire" %% "proxy" % "2.3.0"

libraryDependencies += "com.typesafe" % "config" % "1.3.1"

libraryDependencies += "com.github.kxbmap" %% "configs" % "0.4.4"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"

val buildTime = System.currentTimeMillis()

sourceGenerators in Compile <+=
  (sourceManaged in Compile, name, version, git.gitCurrentBranch, git.gitHeadCommit) map {
    (dir, name, version, currentBranch, headCommit) =>
      val file = dir / "com" / "github" / "dronegator" / "nlp" / "Version.scala"
      IO.write(file,
        s"""
           |package com.github.dronegator.nlp.main
           |
                 |object Version extends VersionTools {
           |  val name = "${name}"
           |  val version = "${version}"
           |  val branch = "${currentBranch}"
           |  val commit = "${headCommit getOrElse "Unknown"}"
           |  val buildTime = "$buildTime"
           |}
            """.stripMargin)
      Seq(file)
  }


//enablePlugins(ScalaKataPlugin)