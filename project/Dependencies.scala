import sbt._

object Dependencies {
  val defaultDependencies = Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
    "junit" % "junit" % "4.12" % "test",
    //  "org.ccil.cowan.tagsoup" % "tagsoup" % "1.2.1",
    "com.beachape" %% "enumeratum" % "1.5.10",
    "jline" % "jline" % "2.14.3",
    "com.softwaremill.macwire" %% "macros" % "2.3.0",
    "com.softwaremill.macwire" %% "util" % "2.3.0",
    "com.softwaremill.macwire" %% "proxy" % "2.3.0",
    "com.chuusai" %% "shapeless" % "2.3.2"
  )

  val serviceDependencies = Seq(
    "com.typesafe" % "config" % "1.3.1",
    "com.github.kxbmap" %% "configs" % "0.4.4",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0"
  )

  val akkaDependencies = Seq(
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.17",
    "com.typesafe.akka" %% "akka-stream" % "2.4.17",
    "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.17",
    "com.typesafe.akka" %% "akka-testkit" % "2.4.17"
  )
}
