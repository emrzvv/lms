ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.13"

lazy val root = (project in file("."))
  .settings(
    name := "lms"
  )

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

val AkkaVersion = "2.9.2"
val AkkaHttpVersion = "10.6.2"
val json4sVersion = "4.0.7"
val jwtVersion = "10.0.1"
val slickPgVersion = "0.22.2"

libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % "3.5.0",
  "org.postgresql" % "postgresql" % "42.7.3",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.5.1",
  "com.github.tminglei" %% "slick-pg" % slickPgVersion,
  "com.github.tminglei" %% "slick-pg_play-json" % slickPgVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.6",
  "org.json4s" %% "json4s-native" % json4sVersion,
  "org.json4s" %% "json4s-jackson" % json4sVersion,
  "com.github.tminglei" %% "slick-pg_json4s" % "0.22.2",
  "de.heikoseeberger" %% "akka-http-json4s" % "1.39.2",
  "org.webjars.npm" % "bootstrap" % "5.3.3",
  "org.webjars.npm" % "bootstrap-icons" % "1.11.3",
  "org.webjars" % "popper.js" % "2.11.7",
  "org.mdedetrich" %% "akka-http-webjars" % "0.5.0",
  "com.github.jwt-scala" %% "jwt-core"  % jwtVersion,
  "com.typesafe.play" %% "twirl-api" % "1.6.8",
  "com.github.t3hnar" %% "scala-bcrypt" % "4.3.0"
)

enablePlugins(SbtTwirl)
enablePlugins(RevolverPlugin)