package de.dani09.mediaio.config

import java.io.File
import java.nio.file.Path

import scala.io.Source

case class CLIConfig(
                      configPath: Path = null,
                      interactive: Boolean = false,
                      diff: Boolean = false,
                      serveWebFrontend: Boolean = false,
                      serverPort: Int = 8080,
                      remoteServer: String = null
                    )

object CLIConfig {
  //noinspection SpellCheckingInspection
  def parse(args: Array[String]): CLIConfig = {
    val parser = new scopt.OptionParser[CLIConfig]("Medio") {
      head("Medio", getVersion)

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

      opt[String]('r', "remote")
        .valueName("<url>")
        .text("Execute download actions on a remote server")
        .action((v, c) => c.copy(remoteServer = v))

      note("")
      cmd("serve")
        .text("Serve the html frontend to watch the downloaded movies in the browser BETA!")
        .action((_, c) => c.copy(serveWebFrontend = true))
        .children(
          opt[Int]('p', "port")
            .text("Sets the port that the server should run on. Default is 80")
            .action((v, c) => c.copy(serverPort = v))
            .validate(v => if ((1 to 65535).contains(v)) success else failure("Port is not valid! must be between 1 and 65535")),

          opt[Unit]('r', "remote")
            .text("Allow remote connection to this server to be able to download movies remotly ALPHA! USE WITH YOUR OWN RISK." +
              " Anyone connecting to the server will be able to download any file")
            .action((_, c) => c.copy(remoteServer = "enabled"))
        )
      note("")

      checkConfig(c =>
        if (c.serveWebFrontend && c.interactive) failure("Cannot start interactive mode and serve the html frontend at the same time!")
        else success
      )

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