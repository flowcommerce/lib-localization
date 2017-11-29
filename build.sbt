lazy val commonSettings = Seq(
  name := "lib-localization",
  organization := "io.flow",
  scalaVersion in ThisBuild := "2.11.11",
  crossScalaVersions := Seq("2.11.11", "2.10.6"),
  resolvers ++= Seq(
    "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    "Artifactory" at "https://flow.artifactoryonline.com/flow/libs-release/"
  ),
  
  credentials += Credentials(
    "Artifactory Realm",
    "flow.artifactoryonline.com",
    System.getenv("ARTIFACTORY_USERNAME"),
    System.getenv("ARTIFACTORY_PASSWORD")
  ),

  publishTo := {
    val host = "https://flow.artifactoryonline.com/flow"
    if (isSnapshot.value) {
      Some("Artifactory Realm" at s"$host/libs-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
    } else {
      Some("Artifactory Realm" at s"$host/libs-release-local")
    }
  },

  libraryDependencies ++=
    Seq(
      "com.gilt" %% "gfc-cache" % "0.0.3",
      "io.flow" %% "lib-reference-scala" % "0.1.30",
      "javax.inject" % "javax.inject" % "1",
      "com.twitter" %% "finagle-redis" % "6.30.0",
      "org.msgpack" % "msgpack-core" % "0.7.1",
      "org.msgpack" % "jackson-dataformat-msgpack" % "0.7.1",
      "org.mockito" % "mockito-core" % "2.8.47" % "test",
      "org.scalatest" %% "scalatest" % "3.0.3" % "test"
    ),
version := "0.0.40"
)

def generateProject(projectName: String, playVersion: String) = {
  Project(projectName, file(s"target/$projectName"))
    .settings(commonSettings)
    .settings(Seq(
      name := projectName,
      libraryDependencies += "com.typesafe.play" %% "play-json" % playVersion,
      scalaSource in Compile := baseDirectory.value / ".." / ".." / "src" / "main" / "scala",
      scalaSource in Test    := baseDirectory.value / ".." / ".." / "src" / "test" / "scala"
    ))
}

lazy val libLocalizationPlay23 = generateProject("lib-localization-play2_3", "2.3.10")
lazy val root =
  (project in file("."))
    .settings(commonSettings)
    .settings(libraryDependencies += "com.typesafe.play" %% "play-json" % "2.4.11")
    .aggregate(libLocalizationPlay23)
