package de.dani09.moviedownloader

import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.Scanner

import de.dani09.moviedownloader.config.{Config, MovieFilter}

object InteractiveMode {
  val movieDataSource = new URL(Config.getMovieDataSourceDefaultValue) // TODO respect url in config

  def start(config: Config): Unit = {
    println("Entering interactive mode!")
    val s = new Scanner(System.in)
    val dl = new MovieDownloader(new Config(getPath(config, s),
      minimumSize = 0, minimumLength = 0, maxDaysOld = 0,
      movieFilters = List[MovieFilter](), movieDataSource = movieDataSource))

    Main.saveMovieData(source = movieDataSource)
    val movies = Main.getMovies(dl)

    while (true) {
      println()
      val filter = createMovieFilter(s)
      val matchedMovies = movies.filter(m => filter.matchesMovie(m)).toList

      println(s"${matchedMovies.length} Movies matched entered Filter!")

      displayMovies(dl, s, matchedMovies)
    }
  }

  private def getPath(config: Config, s: Scanner): Path = {
    if (config == null || config.downloadDirectory == null) {
      print("Please enter an download Directory (default is \"./\")")

      val path = Option(s.nextLine()).filterNot(_.isEmpty).getOrElse("./")
      Paths.get(path)
    } else {
      config.downloadDirectory
    }
  }

  private def displayMovies(dl: MovieDownloader, s: Scanner, movies: List[Movie]): Unit = {
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

      downloadMovie(dl, s, movies)
    }
  }

  private def downloadMovie(dl: MovieDownloader, s: Scanner, movies: List[Movie]): Unit = {
    println("Do you want to download one? (0 or empty String if no else id)")
    println("Be sure to be in the download directory as it will download into the current folder")
    val idString = s.nextLine()

    if (idString.length != 0 && idString.forall(_.isDigit)) {
      // is number
      val id = idString.toInt
      val allowedRange = 1 to movies.length

      if (allowedRange.contains(id))
        dl.downloadMovie(movies(id - 1))
    }
  }

  def createMovieFilter(s: Scanner): MovieFilter = {
    println("Creating MovieFilter")
    println("Enter an TvChannel:")
    val channel = s.nextLine()

    println("Enter an SeriesTitle Regex (empty for \".+\"):")
    val seriesTitle = Option(s.nextLine()).filterNot(_.isEmpty).getOrElse(".+").r

    println("Enter an EpisodeTitle Regex (empty for \".+\"):")
    val episodeTitle = Option(s.nextLine()).filterNot(_.isEmpty).getOrElse(".+").r

    println("Creating MovieFilter")
    val filter = new MovieFilter(channel, seriesTitle, episodeTitle)
    filter
  }
}
