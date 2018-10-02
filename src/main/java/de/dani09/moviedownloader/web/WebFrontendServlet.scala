package de.dani09.moviedownloader.web

import java.net.{URL, URLEncoder}

import de.dani09.moviedownloader.config.{CLIConfig, Config, DownloadedMovies}
import de.dani09.moviedownloader.data.Movie
import org.scalatra.ScalatraServlet
import org.slf4j.LoggerFactory

class WebFrontendServlet(conf: Config, cli: CLIConfig) extends ScalatraServlet {

  private val logger = LoggerFactory.getLogger(getClass)

  get("/getMovies") {
    val movies = DownloadedMovies
      .deserialize(conf)
      .getMovies
      .map(m => m.copy(downloadUrl = getMovieUrl(m)))

    new DownloadedMovies(movies).toJson
  }

  def getMovieUrl(m: Movie): URL = {
    val path = m.getRelativeSavePath.toString

    val encoded = path
      .split("/")
      .map(u => URLEncoder.encode(u, "utf8"))
      .map(_.replaceAll("\\+", "%20"))
      .reduceLeft((a, b) => a + "/" + b)

    new URL(fullUrl("/data/" + encoded, includeServletPath = false))
  }
}
