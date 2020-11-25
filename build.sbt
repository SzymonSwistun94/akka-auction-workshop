name := "akka-auction-workshop"

version := "1.0"

scalaVersion := "2.13.3"

lazy val akkaVersion = "2.6.10"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.2.1",
  "org.scalatest" %% "scalatest" % "3.2.3" % "test"
)
