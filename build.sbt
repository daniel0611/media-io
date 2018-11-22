import java.io.FileWriter

name := "movie-downloader"
organization := "de.dani09"

version := "0.1"

crossScalaVersions := Seq("2.11.11", "2.12.6")
useJCenter := true
resolvers += Classpaths.typesafeReleases

assemblyJarName in assembly := "MovieDownloader.jar"

libraryDependencies ++= Seq(
  "de.mediathekview" % "MLib" % "3.0.2",
  "org.json" % "json" % "20180813",
  "de.dani09" % "dani-http" % "0.4.2",
  "me.tongfei" % "progressbar" % "0.7.2",
  "com.github.scopt" %% "scopt" % "3.7.0",

  // scalatra
  "org.json4s" %% "json4s-jackson" % "3.5.2",
  "org.scalatra" %% "scalatra" % "2.6.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3" % "runtime",
  "org.eclipse.jetty" % "jetty-webapp" % "9.4.6.v20170531",
  "org.eclipse.jetty.websocket" % "websocket-server" % "9.4.6.v20170531",
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
