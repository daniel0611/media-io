package de.dani09.moviedownloader

import org.scalatra.ScalatraServlet

class HealthServlet extends ScalatraServlet {
  get("/") {
    "Running Healthy!"
  }
}
