package de.dani09.moviedownloader

import de.dani09.moviedownloader.config.{CLIConfig, Config}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.ScalatraServlet
import org.scalatra.servlet.ScalatraListener
import org.slf4j.LoggerFactory

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

    server.setHandler(context)

    println(s"Starting server on port $port")

    server.start()
    server.join()
  }
}

class WebFrontendMode extends ScalatraServlet {

  private val logger = LoggerFactory.getLogger(getClass)

  get("/") {
    logger.info("Hello World")
    "Hello World"
  }
}
