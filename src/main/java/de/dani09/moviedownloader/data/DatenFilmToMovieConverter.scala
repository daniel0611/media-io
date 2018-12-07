package de.dani09.moviedownloader.data

import java.net.{MalformedURLException, URL}
import java.text.{ParseException, SimpleDateFormat}

import de.mediathekview.mlib.daten.DatenFilm

import scala.language.implicitConversions

//noinspection SpellCheckingInspection
class DatenFilmToMovieConverter(mov: DatenFilm) {
  private lazy val releaseDateParser = new SimpleDateFormat("hh:mm:ss dd.MM.yyyy")

  def toMovie: Movie = {
    val arr = proccessArray(mov.arr)
    if (arr.size < 8) {
      return null
    }

    try {
      val releaseDateString = arr(3)
      val releaseTimeString = arr(4)

      val releaseDate = releaseDateParser.parse(s"$releaseTimeString $releaseDateString")

      new Movie(
        downloadUrl = new URL(mov.getUrlFuerAufloesung(DatenFilm.AUFLOESUNG_HD)),
        tvChannel = arr.head,
        seriesTitle = arr(1),
        episodeTitle = arr(2),
        releaseDate = releaseDate,
        description = arr(7),
        lengthInMinutes = mov.dauerL / 60,
        sizeInMb = mov.dateigroesseL.l.toInt
      )
    } catch {
      case _: ParseException => null
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

//noinspection SpellCheckingInspection
object DatenFilmToMovieConverter {
  implicit def datenFilm2DatenFilmToMovieConverter(value: DatenFilm): DatenFilmToMovieConverter = new DatenFilmToMovieConverter(value)
}
