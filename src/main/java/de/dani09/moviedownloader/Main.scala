package de.dani09.moviedownloader

import java.net.URL
import java.nio.file.{Path, Paths}

import de.dani09.moviedownloader.config.{CLIConfig, Config}
import org.json.JSONException

import scala.collection.parallel.ParSeq

// TODO add readme with explanations

object Main {
  val movieListFileName = "movie-data.xz"

  def main(args: Array[String]): Unit = {
    val cliConf = CLIConfig.parse(args)
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

    if (config != null)
      println(s"Parsed Config from ${"\"" + cliConf.configPath + "\""} successfully")

    if (cliConf.interactive) {
      InteractiveMode.start(config)
      System.exit(0)
    }

    if (config == null) {
      println("Config file does not exist!")
      System.exit(1)
    }

    println("Launching Movie Downloader")
    downloadMovies(config)
  }

  def downloadMovies(config: Config): Unit = {
    val downloader = new MovieDownloader(config)
    saveMovieData(downloader = downloader, source = config.movieDataSource)

    var movies: ParSeq[Movie] = getMovies(downloader)

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

    movies.toList
      .foreach(x => {
        println(s"[${movies.indexOf(x) + 1}/${movies.length}] Will download following Movie:")
        x.printInfo()
        downloader.downloadMovie(x)
        println()
      })

    println("Done!")
  }

  def saveMovieData(path: Path = getMovieListTmpPath,
                    downloader: MovieDownloader = new MovieDownloader(null),
                    source: URL): Unit = {
    downloader.saveMovieData(getMovieListTmpPath, source)
  }

  def getMovieListTmpPath: Path = {
    val tmp = System.getProperty("java.io.tmpdir")

    Paths.get(tmp, movieListFileName)
  }

  def getMovies(downloader: MovieDownloader, path: Path = getMovieListTmpPath): ParSeq[Movie] = {
    println("Reading Movie Data")
    val movies = downloader.getMovieList(path)
    println("Parsed Movie Data successfully")
    println(s"${movies.length} Movies found in total")

    movies
  }
}
