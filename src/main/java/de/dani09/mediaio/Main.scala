package de.dani09.mediaio

import java.io.{OutputStream, PrintStream}
import java.nio.file.{Path, Paths}

import de.dani09.mediaio.config.{CLIConfig, Config, DownloadedMovies}
import de.dani09.mediaio.data.{Movie, MovieGroupTransformer}
import de.dani09.mediaio.web.WebFrontendMode
import org.json.JSONException

import scala.collection.parallel.ParSeq

object Main {
  val movieListFileName = "movie-data.xz"

  def main(args: Array[String]): Unit = {
    registerFilteredPrintStream()
    var cliConf = CLIConfig.parse(args)
    var config: Config = null

    try {
      config = Config.parseConfig(cliConf.configPath)
    } catch {
      case e: JSONException =>
        println(s"Couldn't parse config at ${"\"" + cliConf.configPath + "\""}: $e")
        System.exit(1)
      case e: Throwable =>
        println(s"Couldn't load config at ${"\"" + cliConf.configPath + "\""}: $e")
        System.exit(1)
    }

    if (config != null) {
      if (config.remote != null && cliConf.remoteServer == null)
        cliConf = cliConf.copy(remoteServer = config.remote)

      println(s"Parsed config from ${"\"" + cliConf.configPath + "\""} successfully")

      if (config.remote == null)
        new MovieGroupTransformer(config).migrateAll()
    }

    if (cliConf.interactive) {
      InteractiveMode.start(config, cliConf)
      System.exit(0)
    }

    if (config == null) {
      println(s"No config file found at ${cliConf.configPath}")
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

    println(s"${movies.length} movies matched filters")

    movies = movies
      .filter(x => !downloader.isMovieAlreadyDownloaded(x))
      .toList
      .sortBy(_.releaseDate.getTime)
      .par
    println(s"${movies.length} not already downloaded")

    System.gc()

    if (movies.nonEmpty) {
      println(s"Will download ${movies.length} movies:")
      movies.foreach(m => println(s"${m.tvChannel} --> ${m.seriesTitle} --> ${m.episodeTitle}"))
      println()
    } else {
      println("No new movies to download!")
    }

    movies.toList
      .foreach(mov => {
        println(s"[${movies.indexOf(mov) + 1}/${movies.length}] Will download following movie:")
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
    println("Reading movie data")
    val movies = downloader.getMovieList(path)
    println("Parsed movie data successfully")
    println(s"${movies.length} movies found in total")

    movies
  }

  /**
    * Creates and sets a PrintStream that filters all write calls from de.mediathekview.mlib
    */
  def registerFilteredPrintStream(): Unit = {
    val original = System.out
    //noinspection ConvertExpressionToSAM Scala 2.11
    val filteredStream = new PrintStream(new OutputStream() {
      override def write(b: Int): Unit = {
        val trace = Thread.currentThread().getStackTrace
        val count = trace.count(_.getClassName.startsWith("de.mediathekview.mlib"))

        if (count < 1)
          original.write(b)
      }
    })

    System.setOut(filteredStream)
  }
}
