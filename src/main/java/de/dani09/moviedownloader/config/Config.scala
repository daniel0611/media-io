package de.dani09.moviedownloader.config

import java.nio.file.{Files, Path, Paths}
import java.util.Date

import de.dani09.moviedownloader.Movie
import org.json.JSONObject

import scala.collection.JavaConverters._

class Config(
              val downloadDirectory: Path,
              val minimumSize: Int,
              val minimumLength: Long,
              val maxDaysOld: Int,
              val movieFilters: List[MovieFilter]
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
    val configString = Files.readAllLines(path).asScala.mkString("")
    val configJson = new JSONObject(configString)

    // Get Filters
    val filterArray = configJson.getJSONArray("filters")
    val filters = (for (i <- 0 until filterArray.length()) yield i)
      .map(index => filterArray.getJSONObject(index))
      .map(f => new MovieFilter(f.getString("tvChannel"), f.getString("seriesTitle").r))
      .toList

    new Config(
      downloadDirectory = Paths.get(configJson.getString("downloadDirectory")),
      minimumSize = configJson.getInt("minimumSize"),
      minimumLength = configJson.getLong("minimumLength"),
      maxDaysOld = configJson.getInt("maxDaysOld"),
      filters
    )
  }
}
