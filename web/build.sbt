name := "web"

version := "0.4"

scalaVersion := "2.11.9"

dependencyUpdatesExclusions := moduleFilter(organization = ScalaArtifacts.Organization)

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.6"

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http-core" % "10.0.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.0.5"

libraryDependencies += "com.typesafe.akka" %% "akka-http-xml" % "10.0.5"

libraryDependencies += "jline" % "jline" % "2.14.3"

libraryDependencies += "io.spray" %% "spray-json" % "1.3.3"

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