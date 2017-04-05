name := "wordmetrix"

version := "0.4"

scalaVersion := "2.11.9"

dependencyUpdatesExclusions := moduleFilter(organization = ScalaArtifacts.Organization)

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