package de.dani09.moviedownloader

import java.io.{File, FileOutputStream, PrintStream}
import java.net.{URL, URLEncoder}
import java.nio.file.{Files, Path}

import de.dani09.http.{Http, HttpProgressListener}
import de.dani09.moviedownloader.config.{CLIConfig, Config}
import de.dani09.moviedownloader.data.DatenFilmToMovieConverter._
import de.dani09.moviedownloader.data.Movie
import de.dani09.moviedownloader.data.ProgressBarBuilder2HttpProgressListener._
import de.dani09.moviedownloader.web.RemoteConnectionClient
import de.mediathekview.mlib.daten.ListeFilme
import de.mediathekview.mlib.filmesuchen.{ListenerFilmeLaden, ListenerFilmeLadenEvent}
import de.mediathekview.mlib.filmlisten.FilmlisteLesen
import me.tongfei.progressbar.{ProgressBarBuilder, ProgressBarStyle}
import org.json.{JSONArray, JSONException}

import scala.collection.JavaConverters._
import scala.collection.parallel.ParSeq
import scala.io.Source


class MovieDownloaderUtil(config: Config, out: PrintStream = System.out, cli: CLIConfig = new CLIConfig()) {

  def saveMovieData(destination: Path, diff: Boolean, listener: HttpProgressListener = getProgressBarHttpListener): Unit = {
    val downloadUrl = if (diff) config.movieDataDiffSource else config.movieDataSource

    downloadFile(destination, downloadUrl, "Movie list")
  }

  def isMovieAlreadyDownloaded(movie: Movie): Boolean = {
    if (isRemote) {
      val rawPath = movie.getRelativeSavePath.toString
      val rawUrl = movie.downloadUrl.toURI.toString
      val path = URLEncoder.encode(rawPath, "utf8")
      val url = URLEncoder.encode(rawUrl, "utf8")

      val response = Http.get(s"http://${cli.remoteServer}/api/isMovieDownloaded?path=$path&url=$url").execute()

      response.getResponseString match {
        case "true" => true
        case "false" => false
        case e: String =>
          out.println("Could not find out if movie is downloaded on remote: ")
          out.println(e)
          false
      }
    } else {
      val file = getMovieSavePath(movie).toFile
      isFileUpToDate(file, movie.downloadUrl)
    }
  }

  //noinspection SpellCheckingInspection
  def getMovieList(movieDataPath: Path): ParSeq[Movie] = {
    val l = new FilmlisteLesen()

    var done = false
    l.addAdListener(new ListenerFilmeLaden() {
      override def fertig(e: ListenerFilmeLadenEvent): Unit = {
        super.fertig(e)
        done = true
      }
    })

    val filme = new ListeFilme()
    l.readFilmListe(movieDataPath.toString, filme, config.maxDaysOld)

    // Waiting until done
    while (!done) {
      Thread.sleep(100)
    }

    // Convert from "DatenFilm" to Movie
    val movies = filme.asScala
      .par
      .map(m => m.toMovie)
      .filter(_ != null)

    movies ++ getIncludedMovies.par // add manually included Movies
  }

  def isFileUpToDate(file: File, url: URL): Boolean = {
    try {
      val connection = url.openConnection()
      val fullSize = connection.getContentLengthLong

      file.length() >= fullSize
    } catch {
      case _: Throwable => false
    }
  }

  private def isRemote: Boolean = cli.remoteServer != null && cli.remoteServer.nonEmpty

  private def getStandardProgressBar: ProgressBarBuilder = {
    new ProgressBarBuilder()
      .setStyle(ProgressBarStyle.ASCII)
      .setUnit("MB", 1048576)
      .setUpdateIntervalMillis(1000)
      .showSpeed()
  }

  private def getProgressBarHttpListener: HttpProgressListener = getStandardProgressBar.toHttpProgressListener

  private def downloadFile(destination: Path, downloadUrl: URL, nameForProgress: String, progressNameQuoted: Boolean = false, listener: HttpProgressListener = getProgressBarHttpListener): Unit = {
    var outStream: FileOutputStream = null
    val taskName = if (progressNameQuoted) "\"" + nameForProgress + "\"" else nameForProgress

    try {
      Files.createDirectories(destination.getParent)
      val file = new File(destination.toUri)
      if (isFileUpToDate(file, downloadUrl)) {
        out.println(s"$taskName is already up-to-date")
        return
      }
      outStream = new FileOutputStream(file)

      out.println(s"Downloading $taskName")

      Http.get(downloadUrl.toString)
        .setOutputStream(outStream)
        .addProgressListener(listener)
        .handleRedirects(10)
        .execute()

    } finally {
      if (outStream != null) {
        outStream.close()
      }
    }
  }

  private def getMovieSavePath(movie: Movie) = {
    movie.getSavePath(config.downloadDirectory)
  }

  /**
    * Parses manually included movies from config.downloadDirectory/include.json
    *
    * @return the movies which need to be included.
    *         In case the file doesn't exist or it can't be parsed it will return an empty list instead
    */
  private def getIncludedMovies: List[Movie] = {
    lazy val emptyList = List[Movie]()
    val file = new File(config.downloadDirectory.toString, "include.json") // get file

    if (!file.exists()) {
      return emptyList // return an empty list if the file doesn't exist
    }

    try {
      val jsonString = Source.fromFile(file).getLines().mkString // get file content as a string
      val arr = new JSONArray(jsonString) // parse to JSONArray

      val list = (for (i <- 0 until arr.length()) yield i) // get indices
        .map(index => arr.getJSONObject(index)) // get objects of indices
        .map(json => Movie.fromJson(json)) // parse to Movie
        .toList

      out.println(s"Successfully parsed ${list.length} Movies from include.json")
      list
    } catch {
      case e: JSONException =>
        out.println(s"Couldn't parse include.json: ${e.getMessage}")
        emptyList // also return an empty list if there is an parsing error
    }
  }

  def downloadMovie(movie: Movie, listener: HttpProgressListener = getProgressBarHttpListener): Unit = {
    if (isRemote) {
      val pbb = getStandardProgressBar
        .setInitialMax(movie.sizeInMb * 1048576L)

      new RemoteConnectionClient(movie, listener, cli.remoteServer, pbb).downloadMovieOnRemote()
    } else {
      val destinationPath = getMovieSavePath(movie)
      val name = s"${movie.seriesTitle} - ${movie.episodeTitle}"

      downloadFile(destinationPath, movie.downloadUrl, name, progressNameQuoted = true, listener)
    }
  }
}
