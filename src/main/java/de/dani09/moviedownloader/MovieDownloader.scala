package de.dani09.moviedownloader

import java.io.{BufferedReader, File, FileOutputStream, InputStreamReader}
import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.Calendar

import de.dani09.moviedownloader.config.Config
import de.mediathekview.mlib.daten.ListeFilme
import de.mediathekview.mlib.filmesuchen.{ListenerFilmeLaden, ListenerFilmeLadenEvent}
import de.mediathekview.mlib.filmlisten.FilmlisteLesen
import me.tongfei.progressbar.{ProgressBarBuilder, ProgressBarStyle}

import scala.collection.JavaConverters._

class MovieDownloader(config: Config) {

  def saveMovieData(destination: Path, downloadUrl: URL): Unit = downloadFile(destination, downloadUrl, "Movie Data")

  def isMovieAlreadyDownloaded(movie: Movie): Boolean = {
    val path: Path = getMovieSavePath(movie)
    path.toFile.exists()
  }

  //noinspection SpellCheckingInspection
  def getMovieList(movieDataPath: Path): List[Movie] = {
    val l = new FilmlisteLesen() // Maybe Parse without Library
    println(movieDataPath.toString)
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
      .map(m => DatenFilmToMovieConverter.convert(m))
      .filter(_ != null)
      .toList
  }

  def downloadMovie(movie: Movie): Unit = {
    val destinationPath = getMovieSavePath(movie)
    val name = s"${movie.tvChannel} --> ${movie.seriesTitle} --> ${movie.episodeTitle}"

    println(s"Downloading $name")
    downloadFile(destinationPath, movie.downloadUrl, name)
  }

  private def downloadFile(destination: Path, downloadUrl: URL, nameForProgress: String): Unit = {
    var file: FileOutputStream = null
    var reader: BufferedReader = null

    try {
      file = new FileOutputStream(new File(destination.toUri))

      val connection = downloadUrl.openConnection()
      val fullSize = connection.getContentLengthLong
      val input = connection.getInputStream

      val pb = new ProgressBarBuilder()
        .setTaskName(s"Downloading $nameForProgress")
        .setStyle(ProgressBarStyle.ASCII)
        .setUnit("MB", 1048576)
        .setInitialMax(fullSize)
        .setUpdateIntervalMillis(100)
        .build()

      reader = new BufferedReader(new InputStreamReader(input))

      val data = new Array[Byte](1024)
      var count = 0
      while (count != -1) {
        count = input.read(data, 0, 1024)
        if (count != -1) {
          file.write(data, 0, count)
          pb.stepBy(count)
        } else {
          pb.close()
        }
      }

    } finally {
      if (reader != null) {
        reader.close()
      }

      if (file != null) {
        file.close()
      }
    }
  }

  private def getMovieSavePath(movie: Movie) = {
    val date = movie.releaseDate
    val calendar = Calendar.getInstance()
    calendar.setTime(date)

    val dateString = s"${calendar.get(Calendar.DAY_OF_MONTH)}.${calendar.get(Calendar.MONTH)}.${calendar.get(Calendar.YEAR)}"
    val fileExtension = getFileExtension(movie.downloadUrl)

    val pathString = s"${config.downloadDirectory}/${movie.tvChannel}/${movie.seriesTitle}/${movie.episodeTitle}-$dateString.$fileExtension"
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
