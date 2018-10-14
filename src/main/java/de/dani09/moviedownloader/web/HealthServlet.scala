package de.dani09.moviedownloader.web

import org.scalatra.ScalatraServlet

/**
  * Reports Health status for kubernetes probes
  */
class HealthServlet extends ScalatraServlet {
  get("/") {
    "Running Healthy!"
  }
}
