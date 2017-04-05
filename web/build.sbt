name := "web"

version := "0.4"

scalaVersion := "2.11.9"

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test"

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.5" % "test"

libraryDependencies += "junit" % "junit" % "4.12" % "test"

libraryDependencies += "com.beachape" %% "enumeratum" % "1.5.10"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.4.17"

libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.4.17"

libraryDependencies += "com.typesafe.akka" %% "akka-stream-testkit" % "2.4.17"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "10.0.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http-xml" % "10.0.5"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.4.17"

libraryDependencies += "jline" % "jline" % "2.14.3"

libraryDependencies += "com.softwaremill.macwire" %% "macros" % "2.3.0" % "provided"

libraryDependencies += "com.softwaremill.macwire" %% "util" % "2.3.0"

libraryDependencies += "com.softwaremill.macwire" %% "proxy" % "2.3.0"

libraryDependencies += "io.spray" %% "spray-json" % "1.3.3"

libraryDependencies += "com.chuusai" %% "shapeless" % "2.3.2"

libraryDependencies += "com.github.fommil" %% "spray-json-shapeless" % "1.3.0"

val urls =
  "https://ajax.googleapis.com/ajax/libs/jquery/3.1.0/jquery.min.js" ::
    "https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.0/jquery-ui.min.js" ::
    "https://ajax.googleapis.com/ajax/libs/jqueryui/1.12.0/themes/smoothness/jquery-ui.css" :: Nil

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