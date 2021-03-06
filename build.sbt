name := """quupNotificationsServer"""

version := "2.1.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  evolutions,
  "com.typesafe.play" %% "anorm" % "2.5.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.8",
  "com.github.mehmetakiftutuncu" %% "errors" % "1.1",
  "com.google.firebase" % "firebase-server-sdk" % "[3.0.0,)",
  "org.specs2" %% "specs2-core" % "3.8.4" % Test,
  "org.specs2" %% "specs2-junit" % "3.8.4" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

scalacOptions in Test ++= Seq("-Yrangepos")
