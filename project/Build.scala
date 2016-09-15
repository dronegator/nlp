import java.net.InetAddress

import com.typesafe.sbt.SbtGit.git
import sbt._
import Keys._

import scala.util.Try

object WordmetrixBuild extends Build {
  val Name = "nlp"

  override lazy val settings = super.settings ++ Seq(version := "0.3")

  import settings._

  val buildTime = System.currentTimeMillis()

  val utils =
    Project(id = "utils", base = file("utils")).dependsOn()

  val wordmetrix =
    Project(id = "wordmetrix", base = file("wordmetrix"))
      .settings(
        libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.7",
        libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.4.0"
      )
      .settings(
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
    Project(id = "akka-utils", base = file("akka-utils")).dependsOn(utils, wordmetrix)

  val index =
    Project(id = "index", base = file("index")).dependsOn(wordmetrix)

  val indexStream =
    Project(id = "index-stream", base = file("index-stream")).dependsOn(wordmetrix, akkaUtils)

  val repl =
    Project(id = "repl", base = file("repl")).dependsOn(wordmetrix, akkaUtils, utils)

  val urls =
    "https://ajax.googleapis.com/ajax/libs/jquery/3.1.0/jquery.min.js" ::
      "https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.0/jquery-ui.min.js" ::
      "https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.0/themes/smoothness/jquery-ui.css" :: Nil

  val www =
    Project(id = "web", base = file("web"))
      .settings(
        resourceGenerators in Compile <+=
          (resourceManaged in Compile, name, version) map { (dir, n, v) =>
            for {
              url <- urls.map(new URL(_))
              fileName <- url.getPath.split("/").lastOption
              fileExt <- fileName.split("\\.").lastOption
            } yield {
              val file = dir / "ui" / fileExt / fileName
              println(s"Download $url into $file")
              IO.download(url, file)
              file
            }
          }
      )
      .dependsOn(wordmetrix, akkaUtils, utils)

  lazy val root = Project(Name,
    base = file("."),
    settings = Project.defaultSettings
  ).dependsOn().aggregate(wordmetrix, index, indexStream, repl, akkaUtils, www)
}
