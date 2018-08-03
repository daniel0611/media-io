package de.dani09.moviedownloader

import java.io.{BufferedReader, File, FileOutputStream, InputStreamReader}
import java.net.URL
import java.nio.file.Path

import de.dani09.moviedownloader.config.Config
import de.mediathekview.mlib.daten.ListeFilme
import de.mediathekview.mlib.filmesuchen.{ListenerFilmeLaden, ListenerFilmeLadenEvent}
import de.mediathekview.mlib.filmlisten.FilmlisteLesen
import me.tongfei.progressbar.{ProgressBarBuilder, ProgressBarStyle}

import scala.collection.JavaConverters._

class MovieDownloader(config: Config) {

  def saveMovieData(destination: Path, downloadUrl: URL): Unit = {
    var file: FileOutputStream = null
    var reader: BufferedReader = null

    try {
      file = new FileOutputStream(new File(destination.toUri))

      val connection = downloadUrl.openConnection()
      val fullSize = connection.getContentLengthLong
      val input = connection.getInputStream

      val pb = new ProgressBarBuilder()
        .setTaskName("Downloading Movie Data")
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

  //noinspection SpellCheckingInspection
  def getMovieList(movieDataPath: Path): List[Movie] = {
    val l = new FilmlisteLesen()
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
    l.readFilmListe(movieDataPath.toString, filme, 1)

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

  /*
  def downloadMovie(): Unit = {

  }*/
}
