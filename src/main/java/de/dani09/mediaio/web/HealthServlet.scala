package de.dani09.mediaio.web

import org.scalatra.ScalatraServlet

/**
  * Reports health status for kubernetes probes
  */
class HealthServlet extends ScalatraServlet {
  get("/") {
    "Running Healthy!"
  }
}
