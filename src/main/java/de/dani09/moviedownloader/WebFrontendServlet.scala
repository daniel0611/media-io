package de.dani09.moviedownloader

import de.dani09.moviedownloader.config.{CLIConfig, Config}
import org.scalatra.ScalatraServlet
import org.slf4j.LoggerFactory

class WebFrontendServlet(conf: Config, cli: CLIConfig) extends ScalatraServlet {

  private val logger = LoggerFactory.getLogger(getClass)

  get("/") {
    s"Hello World\nDownloadDir: ${conf.downloadDirectory}"
  }
}
