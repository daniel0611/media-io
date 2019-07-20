package de.dani09.mediaio.web

import de.dani09.mediaio.config.{CLIConfig, Config}
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{DefaultServlet, ServletHolder}
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.ScalatraServlet
import org.scalatra.servlet.ScalatraListener
import org.slf4j.LoggerFactory

object WebFrontendMode {

  private val logger = LoggerFactory.getLogger(getClass)

  /**
    * starts the WebFrontendMode with the Api and Data Servlet
    */
  def start(config: Config, cli: CLIConfig): Unit = {
    logger.info("Entering Web Frontend Mode!")

    val port = cli.serverPort

    val server = new Server(port)
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setResourceBase("src/main/webapp")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")

    mountScalatraServlet(new WebFrontendServlet(config, cli), "/api/*")
    mountScalatraServlet(new HealthServlet, "/health/*")
    mountJettyServlet(getMovieDirectoryServlet(config), "/data/*", context, "FileServerServlet")
    if (cli.remoteServer != null) {
      RemoteConnectionServlet.init(config)
      mountJettyServlet(new ServletHolder(new RemoteConnectionServlet), "/ws/*", context, "RemoteConnectionServlet")
    }

    server.setHandler(context)

    logger.info(s"Starting server on port $port")

    server.start()
    server.join()
  }

  /**
    * creates an Servlet that serves the DownloadDirectory
    *
    * @param config the Config is used to get the DownloadDirectory Path
    * @return returns the created Servlet
    */
  private def getMovieDirectoryServlet(config: Config): ServletHolder = {
    val s = new DefaultServlet()
    val h = new ServletHolder(s)
    h.setInitParameter("resourceBase", config.downloadDirectory.toString)
    h.setInitParameter("pathInfoOnly", "true")
    h
  }

  /**
    * Mounts the given Servlet on to the scalatra server and logs it to the console
    *
    * @param servlet the servlet which should be mounted
    * @param path    on which path the servlet should listen
    */
  private def mountScalatraServlet(servlet: ScalatraServlet, path: String): Unit = {
    logger.info(s"Mounting ${servlet.getClass.getName.split("\\.").last} at $path")
    ScalatraBootstrap.addServlet(servlet, path)
  }

  /**
    * Mounts the given Servlet on to the jetty context and logs it to the console
    *
    * @param servlet the servlet which should be mounted
    * @param path    on which path the servlet should listen
    * @param context the context on which the servlet will be mounted
    * @param name    the name the servlet has. only for logging purposes
    */
  private def mountJettyServlet(servlet: ServletHolder, path: String, context: WebAppContext, name: String): Unit = {
    logger.info(s"Mounting $name at $path")
    context.addServlet(servlet, path)
  }
}
