package de.dani09.moviedownloader.config

import java.io.File
import java.nio.file.Path

import scala.io.Source

case class CLIConfig(
                      configPath: Path = null
                    )

object CLIConfig {
  def parse(args: Array[String]): CLIConfig = {
    val parser = new scopt.OptionParser[CLIConfig]("MovieDownloader") {
      head("MovieDownloader", getVersion)

      opt[File]('c', "config")
        .valueName("<Path>")
        .text("Path to config file Default: ./config.json")
        .action((v, c) => c.copy(v.toPath))
        .validate(f => if (f.exists()) success else failure("Specified Config file does not exist"))
        .required()
        .withFallback(() => new File("./config.json"))

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
      Source.fromInputStream(in).mkString
    else
      "Dev-Version"
  }
}