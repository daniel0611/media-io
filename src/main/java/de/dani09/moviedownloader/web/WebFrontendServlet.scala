package de.dani09.moviedownloader.web

import java.net.{URL, URLEncoder}

import de.dani09.moviedownloader.config.{CLIConfig, Config, DownloadedMovies}
import de.dani09.moviedownloader.data.Movie
import org.scalatra.ScalatraServlet
import org.slf4j.LoggerFactory

class WebFrontendServlet(conf: Config, cli: CLIConfig) extends ScalatraServlet {

  private val logger = LoggerFactory.getLogger(getClass)
  private implicit val servlet: ScalatraServlet = this

  get("/getMovies") {
    val movies = DownloadedMovies
      .deserialize(conf)
      .getMovies
      .map(m => m.copy(downloadUrl = WebFrontendServlet.getMovieUrl(m)))

    new DownloadedMovies(movies).toJson
  }
}

object WebFrontendServlet {
  private def getMovieUrl(m: Movie)(implicit servlet: ScalatraServlet): URL = {
    val path = m.getRelativeSavePath.toString

    val encoded = path
      .split("/")
      .map(u => URLEncoder.encode(u, "utf8"))
      .map(_.replaceAll("\\+", "%20"))
      .reduceLeft((a, b) => a + "/" + b)

    new URL(servlet.fullUrl("/data/" + encoded, includeServletPath = false)(servlet.request, servlet.response))
  }
}
