package de.dani09.moviedownloader

import de.dani09.moviedownloader.config.{CLIConfig, Config}

object WebFrontendMode {
  def start(config: Config, cli: CLIConfig): Unit = {
    println("Starting interactive mode!")
  }
}
