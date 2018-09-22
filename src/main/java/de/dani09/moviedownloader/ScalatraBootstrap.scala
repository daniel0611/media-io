package de.dani09.moviedownloader

import javax.servlet.ServletContext
import org.scalatra._

import scala.collection.mutable.ListBuffer

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    for ((s, p) <- ScalatraBootstrap.servlets) {
      context.mount(s, p)
    }
  }
}

object ScalatraBootstrap {
  private val servlets = ListBuffer[(ScalatraServlet, String)]()

  def addServlet(servlet: ScalatraServlet, urlPattern: String): Unit = servlets += ((servlet, urlPattern))
}
