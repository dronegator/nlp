name := "ml"

version := "0.4"

scalaVersion := "2.11.9"

dependencyUpdatesExclusions := moduleFilter(organization = ScalaArtifacts.Organization)

libraryDependencies += "org.scalanlp" %% "breeze" % "0.12"

libraryDependencies += "org.scalanlp" %% "breeze-natives" % "0.12"

libraryDependencies += "org.scalanlp" %% "breeze-viz" % "0.12"

