package com.github.dronegator.nlp.main

/**
 * Created by cray on 8/30/16.
 */
trait VersionTools {
  def name: String

  def version: String

  def branch: String

  def commit: String

  def buildTime: String

  def versionMessageExtended =
    s"""
       |Wordmetrix based program $name, $version, $branch, $commit
     """.stripMargin

  def versionString = s"$name-$version.$branch-$commit"
}
