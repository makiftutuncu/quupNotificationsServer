name := """quupNotificationsServer"""

version := "2.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
