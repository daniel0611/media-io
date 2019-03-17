package de.dani09.medio.web

import org.scalatra.ScalatraServlet

/**
  * Reports health status for kubernetes probes
  */
class HealthServlet extends ScalatraServlet {
  get("/") {
    "Running Healthy!"
  }
}
