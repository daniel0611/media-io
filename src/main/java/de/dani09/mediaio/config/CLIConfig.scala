package de.dani09.mediaio.config

import java.io.File
import java.nio.file.Path

import scala.io.Source

case class CLIConfig(
                      configPath: Path = null,
                      interactive: Boolean = false,
                      diff: Boolean = false,
                    )

object CLIConfig {
  //noinspection SpellCheckingInspection
  def parse(args: Array[String]): CLIConfig = {
    val parser = new scopt.OptionParser[CLIConfig]("Media-IO") {
      head("Media-IO", getVersion)

      opt[File]('c', "config")
        .valueName("<path>")
        .text("Path to config file default: ./config.json")
        .action((v, c) => c.copy(v.toPath))
        .required()
        .withFallback(() => new File("./config.json"))

      opt[Unit]('i', "interactive")
        .text("Run in interactive mode to test regexes of movie filters and download single movies")
        .action((_, c) => c.copy(interactive = true))

      opt[Unit]('f', "fast")
        .text("Only download the movie list with the newest movies")
        .action((_, c) => c.copy(diff = true))

      version("version").text("Displays the used version")
      help("help").text("Displays this help page")
    }

    parser.parse(args, new CLIConfig()) match {
      case Some(value) =>
        value
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
      "Dev version"
  }
}