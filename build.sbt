ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.19"

lazy val root = (project in file("."))
  .settings(
    name := "ProgettoBigDataBE",
    libraryDependencies ++= Seq(
      // Dipendenze Spark
      "org.apache.spark" %% "spark-core" % "3.3.0",
      "org.apache.spark" %% "spark-sql" % "3.3.0",
      "org.apache.spark" %% "spark-mllib" % "3.3.0", // Per FPGrowth e Machine Learning

     // Dipendenza Cask per Web Server
     "com.lihaoyi" %% "cask" % "0.8.3",
      // Logging (necessario per Spark e debugging)
      "ch.qos.logback" % "logback-classic" % "1.2.11",


      // HTTP Client (sttp)
      "com.softwaremill.sttp.client3" %% "core" % "3.8.3",

      // JSON Parsing (Circe)
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5"

    )
  )