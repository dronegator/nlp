import sbt._

object WordmetrixBuild extends Build {
  val Name = "nlp"

  override lazy val settings = super.settings

  val wordmetrix =
    Project(id="wordmetrix", base=file("wordmetrix")) //.dependsOn(utils)

  val index =
    Project(id="index", base=file("index")).dependsOn(wordmetrix)

  val indexStream =
    Project(id="index-stream", base=file("index-stream")).dependsOn(wordmetrix)

  val repl =
    Project(id="repl", base=file("repl")).dependsOn(wordmetrix)

  lazy val root = Project(Name,
    base = file("."),
    settings = Project.defaultSettings
  ).dependsOn().aggregate(wordmetrix, index, indexStream)
}
