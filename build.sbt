name := "lib-localization"

organization := "io.flow"

scalaVersion in ThisBuild := "2.11.11"

crossScalaVersions := Seq("2.11.11", "2.10.6")

libraryDependencies ++= {

  Seq(
    "com.gilt" %% "gfc-cache" % "0.0.3",
    "com.typesafe.play" %% "play-json" % "2.6.2",
    "io.flow" %% "lib-reference-scala" % "0.1.30",
    "javax.inject" % "javax.inject" % "1",
    "com.twitter" %% "finagle-redis" % "6.30.0",
    "org.mockito" % "mockito-core" % "2.8.47" % "test",
    "org.scalatest" %% "scalatest" % "3.0.3" % "test"
  )
}

resolvers ++= Seq(
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Artifactory" at "https://flow.artifactoryonline.com/flow/libs-release/"
)

credentials += Credentials(
  "Artifactory Realm",
  "flow.artifactoryonline.com",
  System.getenv("ARTIFACTORY_USERNAME"),
  System.getenv("ARTIFACTORY_PASSWORD")
)

publishTo := {
  val host = "https://flow.artifactoryonline.com/flow"
  if (isSnapshot.value) {
    Some("Artifactory Realm" at s"$host/libs-snapshot-local;build.timestamp=" + new java.util.Date().getTime)
  } else {
    Some("Artifactory Realm" at s"$host/libs-release-local")
  }
}
version := "0.0.18"
