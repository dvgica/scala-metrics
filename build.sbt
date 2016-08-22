
lazy val sharedSettings = Seq(
  organization := "com.pagerduty",
  scalaVersion := "2.10.6",
  crossScalaVersions := Seq("2.10.6", "2.11.7"),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  bintrayOrganization := Some("pagerduty"),
  bintrayRepository := "oss-maven",
  publishMavenStyle := true
)

lazy val api = (project in file("api")).
  settings(sharedSettings: _*).
  settings(
    name := "metrics-api"
  )

lazy val dogstatsd = (project in file("dogstatsd")).
  dependsOn(api).
  settings(sharedSettings: _*).
  settings(
    name := "metrics-dogstatsd",
    libraryDependencies ++= Seq(
      "com.indeed" % "java-dogstatsd-client" % "2.0.13",
      "org.scalatest" %% "scalatest" % "2.2.6" % "test",
      "org.mockito" % "mockito-core" % "1.10.19" % "test" // because ScalaMock doesn't work with StatsDClient
    )
  )

lazy val root = Project(
  id = "root",
  base = file("."),
  aggregate = Seq(api, dogstatsd),
  settings = Project.defaultSettings ++ Seq(
    publishLocal := {},
    publish := {},
    publishArtifact := false
  )
)
