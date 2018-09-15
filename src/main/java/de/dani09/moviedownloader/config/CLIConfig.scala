package de.dani09.moviedownloader.config

import java.io.File
import java.nio.file.Path

import scala.io.Source

case class CLIConfig(
                      configPath: Path = null,
                      interactive: Boolean = false,
                      serveWebFrontend: Boolean = false
                    )

object CLIConfig {
  //noinspection SpellCheckingInspection
  def parse(args: Array[String]): CLIConfig = {
    val parser = new scopt.OptionParser[CLIConfig]("MovieDownloader") {
      head("MovieDownloader", getVersion)

      opt[File]('c', "config")
        .valueName("<Path>")
        .text("Path to config file Default: ./config.json")
        .action((v, c) => c.copy(v.toPath))
        .required()
        .withFallback(() => new File("./config.json"))

      opt[Unit]('i', "interactive")
        .text("Run MovieDownloader in interactive mode to test Regexes of Movie Filters and download single Movies")
        .action((v, c) => c.copy(interactive = true))

      opt[Unit]('s', "serve")
        .action((v, c) => c.copy(serveWebFrontend = true))
        .text("Serve the WebFrontend to watch the downloaded Movies in the Browser\n\t\t" +
          "       Will open the Http server on port 80 or the Environment variable \"PORT\" if set")

      checkConfig(c =>
        if (c.serveWebFrontend && c.interactive) failure("Cannot start interactive Mode and serve the WebFrontend at the same Time!")
        else success
      )

      version("version").text("Displays the used Version")
      help("help").text("Displays this help page")
    }

    parser.parse(args, new CLIConfig()) match {
      case Some(value) => value
      case None =>
        System.exit(1)
        null
    }
  }

  private def getVersion: String = {
    val in = getClass.getClassLoader.getResourceAsStream("version.txt")
    if (in != null)
      s"Version ${Source.fromInputStream(in).mkString}"
    else
      "Version Dev"
  }
}