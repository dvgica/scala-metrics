
lazy val sharedSettings = Seq(
  organization := "com.pagerduty",
  scalaVersion := "2.11.11",
  crossScalaVersions := Seq("2.11.11", "2.12.2"),
  licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
  bintrayOrganization := Some("pagerduty"),
  bintrayRepository := "oss-maven",
  publishMavenStyle := true,
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % Test
  )
)

lazy val api = (project in file("api")).
  settings(sharedSettings: _*).
  settings(
    name := "metrics-api"
  )

lazy val gauge = (project in file("gauge")).
  dependsOn(api).
  settings(sharedSettings: _*).
  settings(
    name := "metrics-gauge",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "1.7.25"
    )
  )

lazy val dogstatsd = (project in file("dogstatsd")).
  dependsOn(api).
  settings(sharedSettings: _*).
  settings(
    name := "metrics-dogstatsd",
    libraryDependencies ++= Seq(
      "com.indeed" % "java-dogstatsd-client" % "2.0.13",
      "org.mockito" % "mockito-core" % "1.10.19" % "test" // because ScalaMock doesn't work with StatsDClient
    )
  )

// lazy val root = Project(id = "root", base = file(".")).
//   dependsOn(api, gauge, dogstatsd).
//   aggregate(api, gauge, dogstatsd).
//   settings(Defaults.coreDefaultSettings ++ Seq(
//     publishLocal := {},
//     publish := {},
//     publishArtifact := false,
//     bintrayReleaseOnPublish := false
//   )
// )
