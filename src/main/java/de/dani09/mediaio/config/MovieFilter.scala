package de.dani09.mediaio.config

import de.dani09.mediaio.data.{Movie, MovieGrouping}

import scala.util.matching.Regex


class MovieFilter(
                   val tvChannel: String,
                   val seriesTitle: Regex,
                   val episodeTitle: Regex,
                   val groupBy: MovieGrouping.Value
                 ) {

  def matchesMovie(movie: Movie): Boolean = {
    if (tvChannel != "" && tvChannel.toLowerCase() != movie.tvChannel.toLowerCase()) return false

    val matchesSeries = seriesTitle.findFirstIn(movie.seriesTitle).nonEmpty
    val matchesEpisode = episodeTitle.findFirstIn(movie.episodeTitle).nonEmpty

    val matches = matchesSeries && matchesEpisode

    if (matches) {
      movie.groupBy = groupBy
    }

    matches
  }
}
