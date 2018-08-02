package de.dani09.moviedownloader.config

import java.nio.file.Path
import java.util.Date

import de.dani09.moviedownloader.Movie

class Config(
              val downloadDirectory: Path,
              val minimumSize: Int,
              val minimumLength: Long,
              val maxDaysOld: Int,
              val movieFilters: List[MovieFilter]
            ) {

  def matchesMovie(movie: Movie): Boolean = {
    // Check minimumSize and minimum Length
    if (movie.sizeInMb < minimumSize) return false
    if (movie.lengthInMinutes < minimumLength) return false

    // Check maxDaysOld
    val movieMillis = movie.releaseDate.getTime
    val maxDaysOldDelta = maxDaysOld.toLong * 24 * 60 * 60 * 1000
    val minimumAllowedMillis = new Date().getTime - maxDaysOldDelta

    if (movieMillis < minimumAllowedMillis) return false

    // Check Filters
    movieFilters.count(_.matchesMovie(movie)) > 0
  }
}