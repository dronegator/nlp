import java.net.InetAddress

import com.typesafe.sbt.SbtGit.git
import sbt._
import Keys._

import scala.util.Try

object WordmetrixBuild extends Build {
  val Name = "nlp"

  override lazy val settings = super.settings
  import  settings._
  val buildTime = System.currentTimeMillis()

  val utils =
    Project(id="utils", base=file("utils")).dependsOn()

  val wordmetrix =
    Project(id="wordmetrix", base=file("wordmetrix")).
      settings(
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
      ).dependsOn(utils)

  val akkaUtils =
    Project(id="akka-utils", base=file("akka-utils")).dependsOn(utils, wordmetrix)

  val index =
    Project(id="index", base=file("index")).dependsOn(wordmetrix)

  val indexStream =
    Project(id="index-stream", base=file("index-stream")).dependsOn(wordmetrix, akkaUtils)

  val repl =
    Project(id="repl", base=file("repl")).dependsOn(wordmetrix, akkaUtils, utils)

  lazy val root = Project(Name,
    base = file("."),
    settings = Project.defaultSettings
  ).dependsOn().aggregate(wordmetrix, index, indexStream, repl, akkaUtils)
}
