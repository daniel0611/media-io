package de.dani09.moviedownloader

import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.Scanner

import de.dani09.moviedownloader.config.{CLIConfig, Config, MovieFilter}
import org.json.JSONException

// TODO Windows support??
// TODO entry in config file to override movieDataSource
// TODO regex filter for episodeTitle

object Main {
  val movieListFileName = "movie-data.xz"
  val movieDataSource = new URL("http://verteiler1.mediathekview.de/Filmliste-akt.xz")

  def main(args: Array[String]): Unit = {
    val cliConf = CLIConfig.parse(args)
    if (cliConf.interactive) {
      startInteractiveMode()
      System.exit(0)
    }
    var config: Config = null

    try {
      config = Config.parseConfig(cliConf.configPath)
    } catch {
      case e: JSONException =>
        println(s"Couldn't parse Config at ${"\"" + cliConf.configPath + "\""}: $e")
        System.exit(1)
      case e: Throwable =>
        println(s"Couldn't load Config at ${"\"" + cliConf.configPath + "\""}: $e")
        System.exit(1)
    }

    println(s"Parsed Config from ${"\"" + cliConf.configPath + "\""} successfully")

    println("Launching Movie Downloader")
    downloadMovies(config)
  }

  def downloadMovies(config: Config): Unit = {
    val downloader = new MovieDownloader(config)
    saveMovieData(downloader = downloader)

    var movies: List[Movie] = getMovies(downloader)

    movies = movies.filter(x => config.matchesMovie(x))
    println(s"${movies.length} Movies matched Filters")

    movies = movies.filter(x => !downloader.isMovieAlreadyDownloaded(x))
    println(s"${movies.length} not already downloaded")

    if (movies.nonEmpty) {
      println(s"Will download ${movies.length} Movies:")
      movies.foreach(m => println(s"${m.tvChannel} --> ${m.seriesTitle} --> ${m.episodeTitle}"))
      println()
    } else {
      println("No new Movies to download!")
    }

    movies.foreach(x => {
      println(s"[${movies.indexOf(x) + 1}/${movies.length}] Will download following Movie:")
      x.printInfo()
      downloader.downloadMovie(x)
      println()
    })

    println("Done!")
  }

  def startInteractiveMode(): Unit = {
    // TODO WAY WAY too big
    println("Entering interactive mode!")
    val dl = new MovieDownloader(new Config(Paths.get("./"), 0, 0, 0, List[MovieFilter]()))

    saveMovieData()
    val movies = getMovies(dl)
    val s = new Scanner(System.in)

    while (true) {
      println()
      println("Creating MovieFilter")
      println("Enter an TvChannel:")
      val channel = s.nextLine()

      println("Enter an SeriesTitle Regex:")
      val seriesTitle = s.nextLine().r

      println("Creating MovieFilter")

      val filter = new MovieFilter(channel, seriesTitle)
      val matchedMovies = movies.filter(m => filter.matchesMovie(m))

      println(s"${matchedMovies.length} Movies matched entered Filter!")

      if (matchedMovies.nonEmpty) {
        println("Do you want to see them? (Y/n)")
        val answer = s.nextLine()

        if (answer.toLowerCase != "n") {
          matchedMovies.foreach(m => {
            val index = matchedMovies.indexOf(m) + 1
            val length = matchedMovies.length

            println(s"[$index/$length]")
            m.printInfo()
          })

          println("Do you want to download one? (0 or empty String if no else id)")
          println("Be sure to be in the download directory as it will download into the current folder")
          val idString = s.nextLine()

          if (idString.length != 0 && idString.forall(_.isDigit)) {
            // is number
            val id = idString.toInt
            val allowedRange = 1 to matchedMovies.length

            if (allowedRange.contains(id)) {
              println(s"Will download id $id")

              dl.downloadMovie(matchedMovies(id - 1)) // TODO get Path from somewhere and not download into "./"
            }
          }
        }
      } else {
        println("Couldn't find any movies that matched this Filter")
      }
    }
  }

  def saveMovieData(path: Path = getMovieListTmpPath,
                    downloader: MovieDownloader = new MovieDownloader(null),
                    source: URL = movieDataSource): Unit = {
    downloader.saveMovieData(getMovieListTmpPath, movieDataSource)
  }

  def getMovieListTmpPath: Path = {
    val tmp = System.getProperty("java.io.tmpdir")

    Paths.get(tmp, movieListFileName)
  }

  private def getMovies(downloader: MovieDownloader, path: Path = getMovieListTmpPath): List[Movie] = {
    println("Reading Movie Data")
    val movies = downloader.getMovieList(path)
    println("Parsed Movie Data successfully")
    println(s"${movies.length} Movies found in total")

    movies
  }
}
