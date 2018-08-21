package de.dani09.moviedownloader

import java.io.{File, FileOutputStream}
import java.net.URL
import java.nio.file.{Files, Path, Paths}
import java.util.Calendar

import de.dani09.moviedownloader.config.Config
import de.mediathekview.mlib.daten.ListeFilme
import de.mediathekview.mlib.filmesuchen.{ListenerFilmeLaden, ListenerFilmeLadenEvent}
import de.mediathekview.mlib.filmlisten.FilmlisteLesen
import me.tongfei.progressbar.{ProgressBarBuilder, ProgressBarStyle}

import scala.collection.JavaConverters._
import scala.collection.parallel.ParSeq

class MovieDownloader(config: Config) {

  def saveMovieData(destination: Path, downloadUrl: URL): Unit = downloadFile(destination, downloadUrl, "Movie Data")

  def isMovieAlreadyDownloaded(movie: Movie): Boolean = {
    val path: Path = getMovieSavePath(movie)
    val file = path.toFile

    isFileUpToDate(file, movie.downloadUrl)
  }

  //noinspection SpellCheckingInspection
  def getMovieList(movieDataPath: Path): ParSeq[Movie] = {
    val l = new FilmlisteLesen() // TODO parse without Library

    var done = false
    l.addAdListener(new ListenerFilmeLaden() {
      override def fertig(e: ListenerFilmeLadenEvent): Unit = {
        super.fertig(e)
        if (e.fehler)
          println(e.text)
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
    filme.asScala
      .par
      .map(m => DatenFilmToMovieConverter.convert(m))
      .filter(_ != null)
  }

  def downloadMovie(movie: Movie): Unit = {
    val destinationPath = getMovieSavePath(movie)
    val name = s"${movie.seriesTitle} - ${movie.episodeTitle}"

    downloadFile(destinationPath, movie.downloadUrl, name, progressNameQuoted = true)
  }

  private def downloadFile(destination: Path, downloadUrl: URL, nameForProgress: String, progressNameQuoted: Boolean = false): Unit = {
    var out: FileOutputStream = null

    try {
      val connection = downloadUrl.openConnection()
      val fullSize = connection.getContentLengthLong
      val input = connection.getInputStream
      val taskName = if (progressNameQuoted) "\"" + nameForProgress + "\"" else nameForProgress

      Files.createDirectories(destination.getParent)
      val file = new File(destination.toUri)
      if (isFileUpToDate(file, downloadUrl)) {
        println(s"$taskName already exists and has same Length! Will not re-download!")
        return
      }
      out = new FileOutputStream(file)

      println(s"Downloading $taskName")

      val pb = new ProgressBarBuilder()
        .setStyle(ProgressBarStyle.ASCII)
        .setUnit("MB", 1048576)
        .setInitialMax(fullSize)
        .setUpdateIntervalMillis(1000)
        .showSpeed()
        .build()

      val data = new Array[Byte](1024)
      var count = 0

      while (count != -1) {
        count = input.read(data, 0, 1024)
        if (count != -1) {
          out.write(data, 0, count)
          pb.stepBy(count)
        } else {
          pb.close()
        }
      }

    } finally {
      if (out != null) {
        out.close()
      }
    }
  }

  def isFileUpToDate(file: File, url: URL): Boolean = {
    try {
      val connection = url.openConnection()
      val fullSize = connection.getContentLengthLong

      file.length() == fullSize
    } catch {
      case _: Throwable => false
    }
  }

  private def getMovieSavePath(movie: Movie) = {
    val date = movie.releaseDate
    val calendar = Calendar.getInstance()
    calendar.setTime(date)

    val dateString = s"${calendar.get(Calendar.DAY_OF_MONTH)}.${calendar.get(Calendar.MONTH)}.${calendar.get(Calendar.YEAR)}"
    val fileExtension = getFileExtension(movie.downloadUrl)

    val tvChannel = movie.tvChannel
    val seriesTitle = movie.seriesTitle.replaceAll("/", "|")
    val episodeTitle = movie.episodeTitle.replaceAll("/", "|")

    val pathString = s"${config.downloadDirectory}/$tvChannel/$seriesTitle/$episodeTitle-$dateString.$fileExtension"
    Paths.get(pathString)
  }

  private def getFileExtension(url: URL): String = {
    url.getPath
      .split("/")
      .lastOption
      .getOrElse(".mp4")
      .split("\\.")
      .last
  }
}
