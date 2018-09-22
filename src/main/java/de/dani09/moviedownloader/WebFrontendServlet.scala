package de.dani09.moviedownloader

import org.scalatra.ScalatraServlet
import org.slf4j.LoggerFactory

class WebFrontendServlet extends ScalatraServlet {

  private val logger = LoggerFactory.getLogger(getClass)

  get("/api") {
    logger.info("Hello World")
    "Hello World"
  }
}
