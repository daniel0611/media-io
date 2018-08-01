package de.dani09.mlibtest

import java.net.{MalformedURLException, URL}

import de.mediathekview.mlib.daten.DatenFilm

//noinspection SpellCheckingInspection
object DatenFilm2MovieConverter {
  def convert(data: DatenFilm): Movie = {
    val arr = proccessArray(data.arr)
    if (arr.size < 8) {
      return null
    }

    try {
      new Movie(
        downloadUrl = new URL(data.getUrl),
        station = arr.head,
        seriesTitle = arr(1),
        episodeTitle = arr(2),
        releaseDate = arr(3),
        releaseTime = arr(4),
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
