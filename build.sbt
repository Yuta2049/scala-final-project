import sbt.Keys.{libraryDependencies, version}

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.3"

lazy val root = (project in file("."))
  .settings(
    name := "final-project",

    libraryDependencies ++= Dependencies.all
  )
