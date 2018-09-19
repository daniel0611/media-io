package de.dani09.moviedownloader.config

import java.io.File
import java.nio.file.Path

import scala.io.Source

case class CLIConfig(
                      configPath: Path = null,
                      interactive: Boolean = false,
                      serveWebFrontend: Boolean = false,
                      serverPort: Int = 8080
                    )

object CLIConfig {
  //noinspection SpellCheckingInspection
  def parse(args: Array[String]): CLIConfig = {
    val parser = new scopt.OptionParser[CLIConfig]("MovieDownloader") {
      head("MovieDownloader", getVersion)

      opt[File]('c', "config")
        .valueName("<path>")
        .text("Path to config file Default: ./config.json")
        .action((v, c) => c.copy(v.toPath))
        .required()
        .withFallback(() => new File("./config.json"))

      opt[Unit]('i', "interactive")
        .text("Run MovieDownloader in interactive mode to test Regexes of Movie Filters and download single Movies\n")
        .action((_, c) => c.copy(interactive = true))

      cmd("serve")
        .valueName("<number>")
        .text("Serve the WebFrontend to watch the downloaded Movies in the Browser")
        .action((_, c) => c.copy(serveWebFrontend = true))
        .children(
          opt[Int]('p', "port")
            .text("Sets the Port that the Server should run on. Default is 80\n")
            .action((v, c) => c.copy(serverPort = v))
            .validate(v => if ((1 to 65535).contains(v)) success else failure("Port is not valid! must be between 1 and 65535"))
        )

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