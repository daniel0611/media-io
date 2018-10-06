package de.dani09.moviedownloader.web

import java.net.{URL, URLEncoder}

import de.dani09.moviedownloader.config.{CLIConfig, Config, DownloadedMovies}
import de.dani09.moviedownloader.data.Movie
import de.dani09.moviedownloader.web.WebFrontendServlet.LocalDownloadedMovies
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
      .deserialize(conf).withLocalDownloadUrls
      .getMovies

    new DownloadedMovies(movies).toJson
  }

  get("/getOverView") {
    // FIXME document this and make it easier to understand!!!!!!!
    DownloadedMovies
      .deserialize(conf).withLocalDownloadUrls
      .getMovies
      .groupBy(_.tvChannel)
      .mapValues(_.groupBy(_.seriesTitle))
      .mapValues(_.mapValues(_.map(_.toJson)))
      .mapValues(_.mapValues(_.foldLeft(new JSONArray())((arr, item) => arr.put(item))))
      .mapValues(_.foldLeft(new JSONObject())((json, item) => json.put(item._1, item._2)))
      .foldLeft(new JSONObject())((json, item) => json.put(item._1, item._2))
      .toString
  }

  get("/getTvChannels") {
    // TODO comment this
    DownloadedMovies
      .deserialize(conf).withLocalDownloadUrls
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
      .deserialize(conf).withLocalDownloadUrls
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
        .deserialize(conf).withLocalDownloadUrls
        .getMovies
        .filter(_.seriesTitle.toLowerCase == series)
        .map(_.toJson)
        .foldLeft(new JSONArray())((arr, movie) => arr.put(movie))
        .toString
    }
  }
}

object WebFrontendServlet {

  private[web] implicit class LocalDownloadedMovies(dl: DownloadedMovies) {
    def withLocalDownloadUrls(implicit servlet: ScalatraServlet): DownloadedMovies = {
      val moviesWithCorrectUrl = dl
        .getMovies
        .map(m => m.copy(downloadUrl = getMovieUrl(m)))

      new DownloadedMovies(moviesWithCorrectUrl)
    }

    private def getMovieUrl(m: Movie)(implicit servlet: ScalatraServlet): URL = {
      val encoded = m.getRelativeSavePath.toString
        .split("/") // do not encode slashes
        .map(u => URLEncoder.encode(u, "utf8"))
        .map(_.replaceAll("\\+", "%20")) // replace + with %20
        .reduceLeft((a, b) => a + "/" + b) // put url back together

      new URL(servlet.fullUrl("/data/" + encoded, includeServletPath = false)(servlet.request, servlet.response))
    }
  }
}
