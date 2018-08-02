package de.dani09.moviedownloader

import java.net.{MalformedURLException, URL}
import java.text.SimpleDateFormat

import de.mediathekview.mlib.daten.DatenFilm

//noinspection SpellCheckingInspection
object DatenFilmToMovieConverter {
  def convert(data: DatenFilm): Movie = {
    val arr = proccessArray(data.arr)
    if (arr.size < 8) {
      return null
    }

    val releaseDateString = arr(3)
    val releaseTimeString = arr(4)
    val releaseDateParser = new SimpleDateFormat("hh:mm:ss dd.MM.yyyy")
    val releaseDate = releaseDateParser.parse(s"$releaseTimeString $releaseDateString")

    try {
      new Movie(
        downloadUrl = new URL(data.getUrl),
        tvChannel = arr.head,
        seriesTitle = arr(1),
        episodeTitle = arr(2),
        releaseDate = releaseDate,
        description = arr(7),
        lengthInMinutes = data.dauerL,
        sizeInMb = data.dateigroesseL.l.toInt
      )
    } catch {
      case _: MalformedURLException => null
    }
  }

  private def proccessArray(data: Array[String]): List[String] =
    data
      .toList
      .filter(_ != "")
      .filter(!_.endsWith(".mp4"))
      .filter(e => !isUrl(e))

  private def isUrl(url: String): Boolean = {
    if (!url.startsWith("http")) {
      return false
    }

    try {
      new URL(url)
      true
    } catch {
      case _: Throwable => false
    }
  }
}
