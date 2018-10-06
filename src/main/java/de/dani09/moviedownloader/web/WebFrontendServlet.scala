package de.dani09.moviedownloader.web

import java.net.{URL, URLEncoder}

import de.dani09.moviedownloader.config.{CLIConfig, Config, DownloadedMovies}
import de.dani09.moviedownloader.data.Movie
import org.json.{JSONArray, JSONObject}
import org.scalatra.ScalatraServlet
import org.slf4j.LoggerFactory

class WebFrontendServlet(conf: Config, cli: CLIConfig) extends ScalatraServlet {

  private val logger = LoggerFactory.getLogger(getClass)
  private implicit val servlet: ScalatraServlet = this

  before() {
    contentType = "application/json"
  }

  get("/getMovies") {
    val movies = DownloadedMovies
      .deserialize(conf)
      .getMovies
      .map(m => m.copy(downloadUrl = WebFrontendServlet.getMovieUrl(m)))

    new DownloadedMovies(movies).toJson
  }

  get("/getTvChannels") {
    // TODO comment this
    DownloadedMovies
      .deserialize(conf)
      .getMovies
      .map(_.tvChannel)
      .map(_.toUpperCase)
      .groupBy(identity)
      .filter(_._2.nonEmpty)
      .keys
      .foldLeft(new JSONArray())((arr, key) => arr.put(key))
      .toString
  }

  get("/getSeries") {
    val channel = params.getOrElse("tvChannel", "").toUpperCase()

    DownloadedMovies
      .deserialize(conf)
      .getMovies
      .filter(m => channel == "" | channel == m.tvChannel) // all if no channel was specified and otherwise check if channel is the same
      .map(_.toJson)
      .foldLeft(new JSONArray())((arr, movie) => arr.put(movie))
      .toString
  }

  get("/getEpisodes") {
    val series = params.getOrElse("series", "").toLowerCase

    if (series == "") {
      status = 400
      "Please specify a series"
    } else {
      DownloadedMovies
        .deserialize(conf)
        .getMovies
        .filter(_.seriesTitle.toLowerCase == series)
        .map(_.toJson)
        .foldLeft(new JSONArray())((arr, movie) => arr.put(movie))
        .toString
    }
  }

  get("/getOverView") {
    // FIXME document this and make it easier to understand!!!!!!!
    DownloadedMovies
      .deserialize(conf)
      .getMovies
      .groupBy(_.tvChannel)
      .mapValues(_.groupBy(_.seriesTitle))
      .mapValues(_.mapValues(_.map(_.toJson)))
      .mapValues(_.mapValues(_.foldLeft(new JSONArray())((arr, item) => arr.put(item))))
      .mapValues(_.foldLeft(new JSONObject())((json, item) => json.put(item._1, item._2)))
      .foldLeft(new JSONObject())((json, item) => json.put(item._1, item._2))
      .toString
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
