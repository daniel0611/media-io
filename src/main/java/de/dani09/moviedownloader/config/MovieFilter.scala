package de.dani09.moviedownloader.config

import de.dani09.moviedownloader.data.Movie

import scala.util.matching.Regex


class MovieFilter(
                   val tvChannel: String,
                   val seriesTitle: Regex,
                   val episodeTitle: Regex
                 ) {

  def matchesMovie(movie: Movie): Boolean = {
    if (tvChannel != "" && tvChannel.toLowerCase() != movie.tvChannel.toLowerCase()) return false

    val matchesSeries = seriesTitle.findFirstIn(movie.seriesTitle).nonEmpty
    val matchesEpisode = episodeTitle.findFirstIn(movie.episodeTitle).nonEmpty

    matchesSeries && matchesEpisode
  }
}
