package de.dani09.mediaio.data

import java.nio.file.{Files, StandardCopyOption}

import de.dani09.mediaio.config.{Config, DownloadedMovies, MovieFilter}

class MovieGroupTransformer(config: Config) {
  private lazy val dl = DownloadedMovies.deserialize(config)

  def migrateAll(): Unit = {
    val x = dl.getMovies
      .zip(dl.getMovies.map(getMovieFilter))

    val y = x.filter(_._2.isDefined)
      .filter(m => m._1.groupBy != m._2.get.groupBy) // only where grouping is different
    y.foreach(x => migrate(x._1, x._2.get))
  }

  private def migrate(m: Movie, filter: MovieFilter): Unit = {
    val oldPath = m.getSavePath(config.downloadDirectory)
    val newMovie = m.copy(groupBy = filter.groupBy)
    val newPath = newMovie.getSavePath(config.downloadDirectory)

    println(s"Migrating this movie's grouping from ${m.groupBy.toString.toLowerCase} to ${newMovie.groupBy.toString.toLowerCase}!")
    newMovie.printInfo()

    Files.createDirectories(newPath.getParent)
    Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING)

    dl.removeMovie(m)
    dl.addMovie(newMovie)
    dl.serialize(config)
  }

  private def getMovieFilter(m: Movie): Option[MovieFilter] = {
    // copy, because the MovieFilter will set its movie grouping to the movie, and we need it later to compare
    config.movieFilters.find(_.matchesMovie(m.copy()))
  }
}
