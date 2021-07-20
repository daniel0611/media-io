package de.dani09.mediaio.web

import org.scalatra._

import javax.servlet.ServletContext
import scala.collection.mutable.ListBuffer

/**
  * ScalatraBootstrap mounts Servlets for Scalatra
  */
class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext): Unit = {
    for ((servlet, url) <- ScalatraBootstrap.servlets) {
      context.mount(servlet, url)
    }
  }
}

object ScalatraBootstrap {
  private val servlets = ListBuffer[(ScalatraServlet, String)]()

  /**
    * adds an servlet that will be mounted once Scalatra starts
    *
    * @param servlet    the Servlet you want to mount
    * @param urlPattern the UrlPattern on which the Servlet should be mounted on
    */
  def addServlet(servlet: ScalatraServlet, urlPattern: String): Unit = servlets += ((servlet, urlPattern))
}
