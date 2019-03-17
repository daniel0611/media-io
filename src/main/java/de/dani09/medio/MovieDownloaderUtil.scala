package de.dani09.medio

import java.io.{File, FileOutputStream, PrintStream}
import java.net.{URL, URLEncoder}
import java.nio.file.{Files, Path}
import java.util.concurrent.CountDownLatch

import de.dani09.http.{Http, HttpProgressListener}
import de.dani09.medio.config.{CLIConfig, Config}
import de.dani09.medio.data.DatenFilmToMovieConverter._
import de.dani09.medio.data.Movie
import de.dani09.medio.data.ProgressBarBuilder2HttpProgressListener._
import de.dani09.medio.web.RemoteConnectionClient
import de.mediathekview.mlib.daten.ListeFilme
import de.mediathekview.mlib.filmesuchen.{ListenerFilmeLaden, ListenerFilmeLadenEvent}
import de.mediathekview.mlib.filmlisten.FilmlisteLesen
import me.tongfei.progressbar.{ProgressBarBuilder, ProgressBarStyle}
import org.json.{JSONArray, JSONException}

import scala.collection.JavaConverters._
import scala.collection.parallel.ParSeq
import scala.io.Source


class MovieDownloaderUtil(config: Config, out: PrintStream = System.out, cli: CLIConfig = new CLIConfig()) {

  /**
    * Downloads the movie list from the url specified in the provided config
    *
    * @param destination where the movie data should be stored
    * @param diff        should a faster diff list be used?
    */
  def saveMovieData(destination: Path, diff: Boolean): Unit = {
    val downloadUrl = if (diff) config.movieDataDiffSource else config.movieDataSource

    downloadFile(destination, downloadUrl, "Movie list")
  }

  /**
    * Downloads a file and reports progress of it
    *
    * @param destination        where the file should be stored
    * @param downloadUrl        from where it should be downloaded
    * @param nameForProgress    a name that is display for progress
    * @param progressNameQuoted whether the progress name should be quoted or not
    * @param listener           the progress listener defaults to a progress bar on the console
    */
  private def downloadFile(destination: Path, downloadUrl: URL, nameForProgress: String,
                           progressNameQuoted: Boolean = false, listener: HttpProgressListener = getProgressBarHttpListener): Unit = {
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

  /**
    * Will check if movie is downloaded either on client or on remote server
    *
    * @param movie the movie which should be checked
    * @return a bool whether the movie is downloaded
    */
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

  /**
    * Checks if the provided file is up to date with the provided url
    *
    * @param file the file
    * @param url  the associated url
    * @return a bool whether it is up to date or not
    */
  def isFileUpToDate(file: File, url: URL): Boolean = {
    try {
      val connection = url.openConnection()
      val fullSize = connection.getContentLengthLong

      file.length() >= fullSize
    } catch {
      case _: Throwable => false
    }
  }

  /**
    * Checks if a remote is specified in the cli config
    */
  private def isRemote: Boolean = cli.remoteServer != null && cli.remoteServer.nonEmpty

  private def getStandardProgressBar: ProgressBarBuilder = {
    new ProgressBarBuilder()
      .setStyle(ProgressBarStyle.ASCII)
      .setUnit("MB", 1048576)
      .setUpdateIntervalMillis(1000)
      .showSpeed()
  }

  private def getProgressBarHttpListener: HttpProgressListener = getStandardProgressBar.toHttpProgressListener

  /**
    * Gets the storage path with filename for a specific movie
    */
  private def getMovieSavePath(movie: Movie): Path = {
    movie.getSavePath(config.downloadDirectory)
  }

  /**
    * Parses movie data and adds manually included movies
    *
    * @param movieDataPath where the movie data/list is saved
    * @return all parsed movies
    */
  //noinspection SpellCheckingInspection
  def getMovieList(movieDataPath: Path): ParSeq[Movie] = {
    val l = new FilmlisteLesen()
    val latch = new CountDownLatch(1)

    l.addAdListener(new ListenerFilmeLaden() {
      override def fertig(e: ListenerFilmeLadenEvent): Unit = {
        super.fertig(e)
        latch.countDown()
      }
    })

    val filme = new ListeFilme()
    l.readFilmListe(movieDataPath.toString, filme, config.maxDaysOld)

    // Waiting until done
    latch.await()

    // Convert from "DatenFilm" to Movie
    val movies = filme.asScala
      .par
      .map(m => m.toMovie)
      .filter(_ != null)

    movies ++ getIncludedMovies.par // add manually included movies
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

  /**
    * Downloads movie either on current client or on remote server depended by the config
    *
    * @param movie    the movie which should be downloaded. Needed fore url and filename
    * @param listener the listener for progress. Defaults to a progress bar on the terminal
    */
  def downloadMovie(movie: Movie, listener: HttpProgressListener = getProgressBarHttpListener): Unit = {
    if (isRemote) {
      val pbb = getStandardProgressBar
        .setInitialMax(movie.sizeInMb * 1048576L) // estimated size may be updated by remote

      new RemoteConnectionClient(movie, listener, cli.remoteServer, pbb).downloadMovieOnRemote()
    } else {
      val destinationPath = getMovieSavePath(movie)
      val name = s"${movie.seriesTitle} - ${movie.episodeTitle}"

      downloadFile(destinationPath, movie.downloadUrl, name, progressNameQuoted = true, listener)
    }
  }
}
