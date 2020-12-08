enablePlugins(JavaServerAppPackaging)

name := "ScheduleBackend"

organization := "kz.playforfun"

version := "1.0.0"

scalaVersion := "2.12.8"

lazy val logbackVersion          = "1.2.3"
lazy val akkaHttpVersion         = "10.1.10"
lazy val akkaVersion             = "2.5.26"
lazy val mongoVersion            = "0.18.6"
lazy val json4sVersion           = "3.6.7"
lazy val httpJson4sVersion       = "1.29.1"
lazy val jodaTimeVersion         = "2.10.5"
lazy val bcryptVersion           = "4.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka"      %% "akka-http"                         % akkaHttpVersion,
  "com.typesafe.akka"      %% "akka-actor"                        % akkaVersion,
  "com.typesafe.akka"      %% "akka-stream"                       % akkaVersion,
  "com.typesafe.akka"      %% "akka-slf4j"                        % akkaVersion,
  "org.reactivemongo"      %% "reactivemongo"                     % mongoVersion,
  "org.reactivemongo"      %% "reactivemongo-bson-macros"         % mongoVersion,
  "org.json4s"             %% "json4s-native"                     % json4sVersion,
  "org.json4s"             %% "json4s-jackson"                    % json4sVersion,
  "ch.qos.logback"         %  "logback-classic"                   % logbackVersion,
  "de.heikoseeberger"      %% "akka-http-json4s"                  % httpJson4sVersion,
  "joda-time"              %  "joda-time"                         % jodaTimeVersion,
  "com.github.t3hnar"      %% "scala-bcrypt"                      % bcryptVersion
)

