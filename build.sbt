ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "lms"
  )

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.5.0",
  "org.postgresql" % "postgresql" % "42.7.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.5.1",
  "com.github.tminglei" %% "slick-pg" % "0.22.1",
  "com.github.tminglei" %% "slick-pg_play-json" % "0.22.1"
)