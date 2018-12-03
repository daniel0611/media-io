package de.dani09.moviedownloader

import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.Scanner

import de.dani09.moviedownloader.config.{CLIConfig, Config, DownloadedMovies, MovieFilter}
import de.dani09.moviedownloader.data.Movie

object InteractiveMode {
  def start(config: Config, cli: CLIConfig): Unit = {
    println("Entering interactive mode!")
    val s = new Scanner(System.in)
    val dl = new MovieDownloaderUtil(new Config(getPath(config, s),
      minimumSize = 0, minimumLength = 0, maxDaysOld = 0,
      movieFilters = List[MovieFilter](),
      movieDataSource = getMovieDataSource(config),
      movieDataDiffSource = getMovieDataDiffSource(config), remote = null),
      cli = cli)

    Main.saveMovieData(downloader = dl, diff = cli.diff)
    val movies = Main.getMovies(dl)

    while (true) {
      println()
      val filter = createMovieFilter(s)

      println("Finding matches...")
      val matchedMovies = movies
        .filter(m => filter.matchesMovie(m))
        .filter(_.exists())
        .toList

      println(s"${matchedMovies.length} Movies matched entered Filter!")

      displayMovies(dl, s, matchedMovies, config)
    }
  }

  private def getMovieDataSource(c: Config): URL = {
    if (c == null || c.movieDataSource == null) {
      new URL(Config.getMovieDataSourceDefaultValue)
    } else {
      c.movieDataSource
    }
  }

  private def getMovieDataDiffSource(c: Config): URL = {
    if (c == null || c.movieDataDiffSource == null) {
      new URL(Config.getMovieDataDiffSourceDefaultValue)
    } else {
      c.movieDataDiffSource
    }
  }

  private def getPath(config: Config, s: Scanner): Path = {
    if (config == null || config.downloadDirectory == null) {
      print("Please enter an download Directory (default is \"./\") ")

      val path = Option(s.nextLine()).filterNot(_.isEmpty).getOrElse("./")
      Paths.get(path)
    } else {
      config.downloadDirectory
    }
  }

  private def displayMovies(dl: MovieDownloaderUtil, s: Scanner, movies: List[Movie], config: Config): Unit = {
    if (movies.isEmpty) {
      println("Couldn't find any movies that matched this Filter")
      return
    }

    print("Do you want to see them? (Y/n) ")
    val answer = s.nextLine()

    if (answer.toLowerCase != "n") {

      if (movies.length > 50) {
        print("That are a lot of Movies! Are you sure? (y/N) ")
        val answer = s.nextLine()
        if (answer.toLowerCase != "y") return
      }

      movies.foreach(m => {
        val index = movies.indexOf(m) + 1
        val length = movies.length

        println(s"[$index/$length]")
        m.printInfo()
      })

      downloadMovie(dl, s, movies, config)
    }
  }

  private def downloadMovie(dl: MovieDownloaderUtil, s: Scanner, movies: List[Movie], config: Config): Unit = {
    println("Do you want to download one? (0 or empty String if no else id) ")
    val idString = s.nextLine()

    if (idString.length != 0 && idString.forall(_.isDigit)) {
      // is number
      val id = idString.toInt
      val allowedRange = 1 to movies.length

      if (allowedRange.contains(id)) {
        val movie = movies(id - 1)
        dl.downloadMovie(movie)
        saveMovieToDownloadedList(movie, config)
      } else {
        println("Unknown Id!")
      }
    }
  }

  private def saveMovieToDownloadedList(movie: Movie, config: Config): Unit = {
    val downloadList = DownloadedMovies.deserialize(config)

    downloadList.addMovie(movie)

    downloadList.serialize(config)
  }

  private def createMovieFilter(s: Scanner): MovieFilter = {
    println("Enter an TvChannel:")
    val channel = s.nextLine()

    println("Enter an SeriesTitle Regex (empty to match anything) ")
    val seriesTitle = Option(s.nextLine()).filterNot(_.isEmpty).getOrElse(".+").r

    println("Enter an EpisodeTitle Regex (empty to match anything) ")
    val episodeTitle = Option(s.nextLine()).filterNot(_.isEmpty).getOrElse(".+").r

    val filter = new MovieFilter(channel, seriesTitle, episodeTitle)
    filter
  }
}
