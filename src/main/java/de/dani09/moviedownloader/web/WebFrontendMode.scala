package de.dani09.moviedownloader.web

import de.dani09.moviedownloader.config.{CLIConfig, Config}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletHolder}
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object WebFrontendMode {
  def start(config: Config, cli: CLIConfig): Unit = {
    println("Entering Web Frontend Mode!")

    val port = cli.serverPort

    val server = new Server(port)
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    ScalatraBootstrap.addServlet(new WebFrontendServlet(config, cli), "/api/*")
    ScalatraBootstrap.addServlet(new HealthServlet, "/health/*")
    context.addServlet(getMovieDirectoryServlet(config), "/data/*")

    server.setHandler(context)

    println(s"Starting server on port $port")

    server.start()
    server.join()
  }

  def getMovieDirectoryServlet(config: Config): ServletHolder = {
    val s = new DefaultServlet()
    val h = new ServletHolder(s)
    h.setInitParameter("resourceBase", config.downloadDirectory.toString)
    h.setInitParameter("pathInfoOnly", "true")
    h
  }
}
