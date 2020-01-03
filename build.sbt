import java.io.FileWriter

name := "media-io"
organization := "de.dani09"

version := "0.1"
scalaVersion := "2.12.10"

assemblyJarName in assembly := "Media-IO.jar"

libraryDependencies ++= Seq(
  "de.mediathekview" % "MLib" % "3.0.2",
  "org.json" % "json" % "20180813",
  "de.dani09" % "dani-http" % "0.4.2",
  "me.tongfei" % "progressbar" % "0.7.3",
  "com.github.scopt" %% "scopt" % "3.7.1",

  // scalatra
  "org.json4s" %% "json4s-jackson" % "3.6.5",
  "org.scalatra" %% "scalatra" % "2.6.5",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.15.v20190215",
  "org.eclipse.jetty.websocket" % "websocket-server" % "9.4.15.v20190215",
  "org.eclipse.jetty.websocket" % "websocket-client" % "9.4.15.v20190215",
)

lazy val createVersionFile = taskKey[Unit]("Creates file containing the current version for use by the cli")
createVersionFile := {
  val resourceDir = (resourceDirectory in Compile).value

  val f = new File(resourceDir, "version.txt")
  val writer = new FileWriter(f)

  writer.write(version.value)
  writer.flush()
  writer.close()

  streams.value.log.info("Created version file")
}

// let anything important depend on createVersionFile
compile := ((compile in Compile) dependsOn createVersionFile).value
run := ((run in Compile) dependsOn createVersionFile).evaluated
assembly := (assembly dependsOn createVersionFile).value
