package de.dani09.mediaio.data

import java.nio.file.{Files, NoSuchFileException, StandardCopyOption}

import de.dani09.mediaio.config.{Config, DownloadedMovies, MovieFilter}

class MovieGroupTransformer(config: Config) {
  private lazy val dl = DownloadedMovies.deserialize(config)

  def migrateAll(): Unit = {
    // only where grouping is different
    dl.getMovies
      .zip(dl.getMovies.map(getMovieFilter))
      .filter(_._2.isDefined)
      .filter(m => m._1.groupBy != m._2.get.groupBy)
      .foreach(x => migrate(x._1, x._2.get))
  }

  private def migrate(m: Movie, filter: MovieFilter): Unit = try {
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
  } catch {
    case _: NoSuchFileException =>
    case e: Throwable =>
      println(s"Couldn't migrate this movie's grouping from ${m.groupBy.toString.toLowerCase} to ${filter.groupBy.toString.toLowerCase}!")
      e.printStackTrace()
  }

  private def getMovieFilter(m: Movie): Option[MovieFilter] = {
    // copy, because the MovieFilter will set its movie grouping to the movie, and we need it later to compare
    config.movieFilters.find(_.matchesMovie(m.copy()))
  }
}
