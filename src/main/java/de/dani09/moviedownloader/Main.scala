package de.dani09.moviedownloader

import java.nio.file.{Path, Paths}

import de.dani09.moviedownloader.config.{CLIConfig, Config, DownloadedMovies}
import de.dani09.moviedownloader.data.Movie
import de.dani09.moviedownloader.web.WebFrontendMode
import org.json.JSONException

import scala.collection.parallel.ParSeq

// TODO add readme with explanations

object Main {
  val movieListFileName = "movie-data.xz"

  def main(args: Array[String]): Unit = {
    var cliConf = CLIConfig.parse(args)
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

    if (config != null) {
      if (config.remote != null && cliConf.remoteServer == null)
        cliConf = cliConf.copy(remoteServer = config.remote)

      println(s"Parsed Config from ${"\"" + cliConf.configPath + "\""} successfully")
    }

    if (cliConf.interactive) {
      InteractiveMode.start(config, cliConf)
      System.exit(0)
    }

    if (config == null) {
      println(s"No Config file found at ${cliConf.configPath}")
      System.exit(1)
    }

    if (cliConf.serveWebFrontend) {
      WebFrontendMode.start(config, cliConf.copy(remoteServer = ""))
      System.exit(0)
    }

    downloadMovies(config, cliConf)
  }

  def downloadMovies(config: Config, cli: CLIConfig): Unit = {
    val downloadedMovies = DownloadedMovies.deserialize(config)

    val downloader = new MovieDownloaderUtil(config, cli = cli)
    saveMovieData(downloader = downloader, diff = cli.diff)

    var movies: ParSeq[Movie] = getMovies(downloader)

    movies = movies
      .filter(x => config.matchesMovie(x))
      .filter(_.exists())

    if (cli.remoteServer == null || cli.remoteServer.isEmpty) // only if not connected to remote
      movies.filter(downloader.isMovieAlreadyDownloaded).foreach(downloadedMovies.addMovie) // if they aren't in the list they will be added here

    println(s"${movies.length} Movies matched Filters")

    movies = movies
      .filter(x => !downloader.isMovieAlreadyDownloaded(x))
      .toList
      .sortBy(_.releaseDate.getTime)
      .par
    println(s"${movies.length} not already downloaded")

    System.gc()

    if (movies.nonEmpty) {
      println(s"Will download ${movies.length} Movies:")
      movies.foreach(m => println(s"${m.tvChannel} --> ${m.seriesTitle} --> ${m.episodeTitle}"))
      println()
    } else {
      println("No new Movies to download!")
    }

    movies.toList
      .foreach(mov => {
        println(s"[${movies.indexOf(mov) + 1}/${movies.length}] Will download following Movie:")
        mov.printInfo()

        downloader.downloadMovie(mov)
        println()

        if (cli.remoteServer == null || cli.remoteServer.isEmpty) {
          downloadedMovies.addMovie(mov)
          downloadedMovies.serialize(config)
        }
      })

    println("Done!")
  }

  def saveMovieData(path: Path = getMovieListTmpPath,
                    downloader: MovieDownloaderUtil,
                    diff: Boolean): Unit = {
    downloader.saveMovieData(getMovieListTmpPath, diff)
  }

  def getMovieListTmpPath: Path = {
    val tmp = System.getProperty("java.io.tmpdir")

    Paths.get(tmp, movieListFileName)
  }

  def getMovies(downloader: MovieDownloaderUtil, path: Path = getMovieListTmpPath): ParSeq[Movie] = {
    println("Reading Movie Data")
    val movies = downloader.getMovieList(path)
    println("Parsed Movie Data successfully")
    println(s"${movies.length} Movies found in total")

    movies
  }
}
