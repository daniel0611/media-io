package de.dani09.moviedownloader.config

import de.dani09.moviedownloader.Movie

import scala.util.matching.Regex


class MovieFilter(
                   val tvChannel: String,
                   val seriesTitle: Regex
                 ) {

  def matchesMovie(movie: Movie): Boolean = {

    if (tvChannel != "" && tvChannel.toLowerCase() != movie.tvChannel.toLowerCase()) return false

    val matches = seriesTitle.findFirstIn(movie.seriesTitle)

    matches.nonEmpty
  }
}
