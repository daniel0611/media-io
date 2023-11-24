import java.io.FileWriter

name := "media-io"
organization := "de.dani09"

version := "0.1"
scalaVersion := "3.3.1"

libraryDependencies ++= Seq(
  "de.mediathekview" % "MLib" % "3.0.13",
  "org.json" % "json" % "20231013",
  "de.dani09" % "dani-http" % "0.4.2",
  "me.tongfei" % "progressbar" % "0.10.0",
  "com.github.scopt" %% "scopt" % "4.1.0",
  "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
)

lazy val createVersionFile = taskKey[Unit]("Creates file containing the current version for use by the cli")
createVersionFile := {
  val resourceDir = (Compile / resourceDirectory).value

  val f = new File(resourceDir, "version.txt")
  val writer = new FileWriter(f)

  writer.write(version.value)
  writer.flush()
  writer.close()

  streams.value.log.info("Created version file")
}

// let anything important depend on createVersionFile
compile := ((Compile / compile) dependsOn createVersionFile).value
run := ((Compile / run) dependsOn createVersionFile).evaluated

// Docker setup
// Run sbt docker:stage and then a Dockerfile will be in target/docker/stage/Dockerfile

enablePlugins(JavaAppPackaging)
dockerBaseImage := "eclipse-temurin:17-jre"
Docker / defaultLinuxInstallLocation := "/app"
Docker / daemonUser := "root"
Docker / daemonUserUid := None
