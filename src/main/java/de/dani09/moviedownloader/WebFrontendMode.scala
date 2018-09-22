package de.dani09.moviedownloader

import de.dani09.moviedownloader.config.{CLIConfig, Config}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener

object WebFrontendMode {
  def start(config: Config, cli: CLIConfig): Unit = {
    println("Starting interactive mode!")

    val port = cli.serverPort

    val server = new Server(port)
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    ScalatraBootstrap.addServlet(new WebFrontendServlet(config, cli), "/api/*")

    server.setHandler(context)

    println(s"Starting server on port $port")

    server.start()
    server.join()
  }
}
