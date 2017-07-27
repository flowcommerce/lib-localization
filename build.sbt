name := "lib-localization"

organization := "io.flow"

scalaVersion in ThisBuild := "2.11.11"

crossScalaVersions := Seq("2.11.11", "2.10.6")

lazy val root = project
  .in(file("."))
  .enablePlugins(PlayScala)
  .settings(
    libraryDependencies ++= Seq(
      "io.flow" %% "lib-reference-scala" % "0.1.24",
      "net.debasishg" %% "redisclient" % "3.4",
      "com.gilt" %% "gfc-cache" % "0.0.3",
      "org.scalatest" %% "scalatest" % "3.0.3" % "test",
      "org.mockito" % "mockito-core" % "2.8.47" % "test"
    ),
    resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
    resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
    resolvers += "Artifactory" at "https://flow.artifactoryonline.com/flow/libs-release/",
    credentials += Credentials(
      "Artifactory Realm",
      "flow.artifactoryonline.com",
      System.getenv("ARTIFACTORY_USERNAME"),
      System.getenv("ARTIFACTORY_PASSWORD")
    )
)

publishTo := {
  val host = "https://flow.artifactoryonline.com/flow"
  if (isSnapshot.value) {
    Some("Artifactory Realm" at s"$host/libs-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
  } else {
    Some("Artifactory Realm" at s"$host/libs-release-local")
  }
}
version := "0.0.6"
