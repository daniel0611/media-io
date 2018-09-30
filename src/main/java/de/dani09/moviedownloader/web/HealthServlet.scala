package de.dani09.moviedownloader.web

import org.scalatra.ScalatraServlet

class HealthServlet extends ScalatraServlet {
  get("/") {
    "Running Healthy!"
  }
}
