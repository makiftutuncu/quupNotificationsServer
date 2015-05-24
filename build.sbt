name := """quupNotificationsServer"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  ws,
  "postgresql" % "postgresql" % "9.1-901-1.jdbc4"
)
