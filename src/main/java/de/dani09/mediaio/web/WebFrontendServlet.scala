package de.dani09.mediaio.web

import java.io.{ByteArrayOutputStream, PrintStream}
import java.net.{URL, URLEncoder}
import java.nio.file.Paths

import de.dani09.mediaio.MovieDownloaderUtil
import de.dani09.mediaio.config.{CLIConfig, Config, DownloadedMovies}
import de.dani09.mediaio.data.Movie
import de.dani09.mediaio.web.WebFrontendServlet.LocalDownloadedMovies
import org.json.JSONArray
import org.scalatra.ScalatraServlet

class WebFrontendServlet(conf: Config, cli: CLIConfig) extends ScalatraServlet {

  private implicit val servlet: ScalatraServlet = this
  private lazy val mdu = new MovieDownloaderUtil(conf, new PrintStream(new ByteArrayOutputStream()), cli)

  before() {
    contentType = "application/json"
  }

  /**
    * Sends all downloaded movies as json
    */
  get("/getMovies") {
    val movies = DownloadedMovies
      .deserialize(conf).withLocalDownloadUrls
      .getMovies

    new DownloadedMovies(movies).toJson
  }

  /**
    * Returns all tvChannels in an sorted json array
    */
  get("/getTvChannels") {
    // TODO comment this
    DownloadedMovies
      .deserialize(conf).withLocalDownloadUrls
      .getMovies
      .map(_.tvChannel)
      .map(_.toUpperCase) // get all tvChannels in uppercase
      .distinct // remove duplicates
      .sorted
      .foldLeft(new JSONArray())((arr, key) => arr.put(key)) // put into json array
      .toString
  }

  /**
    * Returns all series of a specific tvChannel as a sorted json array
    */
  get("/getSeries") {
    val channel = params.getOrElse("channel", "").toUpperCase()

    if (channel.isEmpty) {
      status = 400
      "Please specify a tvChannel"
    } else {
      DownloadedMovies
        .deserialize(conf).withLocalDownloadUrls
        .getMovies
        .filter(m => channel == m.tvChannel.toUpperCase) // check if channel is the same
        .map(_.seriesTitle)
        .distinct
        .sorted
        .foldLeft(new JSONArray())((arr, movie) => arr.put(movie)) // put into json array
        .toString
    }
  }

  /**
    * Returns all episodes of a specific series sorted by release date
    */
  get("/getEpisodes") {
    val series = params.getOrElse("series", "").toLowerCase

    if (series.isEmpty) {
      status = 400
      "Please specify a series"
    } else {
      DownloadedMovies
        .deserialize(conf).withLocalDownloadUrls
        .getMovies
        .filter(_.seriesTitle.toLowerCase == series)
        .sortBy(_.releaseDate.getTime)
        .map(_.toJson)
        .foldLeft(new JSONArray())((arr, movie) => arr.put(movie))
        .toString
    }
  }

  get("/isMovieDownloaded") {
    val path = params.getOrElse("path", "")
    val url = params.getOrElse("url", "")

    if (path.isEmpty) {
      status = 400
      "Path not defined"
    } else if (url.isEmpty) {
      status = 400
      "Url not defined"
    } else {
      val moviePath = Paths.get(path)
      val f = conf.downloadDirectory.resolve(moviePath).toFile

      mdu.isFileUpToDate(f, new URL(url))
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
        .map(_.replaceAll("\\+", "%20")) // replace + with %20 (proper whitespaces)
        .reduceLeft((a, b) => a + "/" + b) // put url back together

      new URL(servlet.fullUrl("/data/" + encoded, includeServletPath = false)(servlet.request, servlet.response))
    }
  }

}
