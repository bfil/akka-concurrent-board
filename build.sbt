name := """concurrent-board"""

version := "0.1"

scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-remote" % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3",
  "io.spray" % "spray-can" % "1.2-RC2",
  "io.spray" % "spray-routing" % "1.2-RC2",
  "io.backchat.hookup" %% "hookup" % "0.2.3",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test"
)

exportJars := true

seq(Revolver.settings: _*)