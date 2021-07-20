package de.dani09.mediaio.config

import de.dani09.mediaio.data.{Movie, MovieGrouping}
import org.json.JSONObject

import java.io.File
import java.net.URL
import java.nio.file.{Files, Path, Paths}
import java.util.Date
import scala.jdk.CollectionConverters._

class Config(
              val downloadDirectory: Path,
              val minimumSize: Int,
              val minimumLength: Long,
              val maxDaysOld: Int,
              val movieFilters: List[MovieFilter],
              val movieDataSource: URL,
              val movieDataDiffSource: URL,
              val remote: String
            ) {

  def matchesMovie(movie: Movie): Boolean = {
    // Check minimumSize and minimum Length
    if (minimumSize != 0 && movie.sizeInMb != 0
      && movie.sizeInMb < minimumSize) return false
    if (minimumLength != 0 && movie.lengthInMinutes != 0
      && movie.lengthInMinutes < minimumLength) return false

    // Check maxDaysOld
    val movieMillis = movie.releaseDate.getTime
    val maxDaysOldDelta = if (maxDaysOld == 0) Long.MaxValue else maxDaysOld.toLong * 24 * 60 * 60 * 1000
    val minimumAllowedMillis = new Date().getTime - maxDaysOldDelta

    if (movieMillis < minimumAllowedMillis) return false

    // Check Filters
    movieFilters.count(_.matchesMovie(movie)) > 0
  }
}

object Config {
  def parseConfig(path: Path): Config = {
    if (!new File(path.toUri).exists()) {
      return null
    }

    val configString = Files.readAllLines(path).asScala.mkString("")
    val configJson = new JSONObject(configString)

    // Get Filters
    val filterArray = configJson.getJSONArray("filters")
    val filters = (for (i <- 0 until filterArray.length()) yield i)
      .map(index => filterArray.getJSONObject(index))
      .map(f => parseMovieFilter(f))
      .toList

    new Config(
      downloadDirectory = Paths.get(configJson.getString("downloadDirectory")),
      minimumSize = configJson.optInt("minimumSize", 0),
      minimumLength = configJson.optLong("minimumLength", 0),
      maxDaysOld = configJson.optInt("maxDaysOld", 0),
      movieFilters = filters,
      movieDataSource = new URL(configJson.optString("movieDataSource", getMovieDataSourceDefaultValue)),
      movieDataDiffSource = new URL(configJson.optString("movieDataSourceDiff", getMovieDataSourceDefaultValue)),
      remote = configJson.optString("remote", null)
    )
  }

  private def parseMovieFilter(j: JSONObject): MovieFilter = new MovieFilter(
    tvChannel = j.optString("tvChannel", ""),
    seriesTitle = j.optString("seriesTitle", ".+").r,
    episodeTitle = j.optString("episodeTitle", ".+").r,
    groupBy = MovieGrouping.parse(j.optString("groupBy", ""))
  )

  def getMovieDataSourceDefaultValue = "https://verteiler1.mediathekview.de/Filmliste-akt.xz"

  def getMovieDataDiffSourceDefaultValue = "https://verteiler1.mediathekview.de/Filmliste-diff.xz"
}
