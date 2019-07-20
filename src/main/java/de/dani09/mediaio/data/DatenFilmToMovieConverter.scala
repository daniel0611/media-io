package de.dani09.mediaio.data

import java.net.{MalformedURLException, URL}
import java.text.{ParseException, SimpleDateFormat}

import de.mediathekview.mlib.daten.DatenFilm

import scala.language.implicitConversions

//noinspection SpellCheckingInspection
class DatenFilmToMovieConverter(mov: DatenFilm) {
  private lazy val releaseDateParser = new SimpleDateFormat("hh:mm:ss dd.MM.yyyy")

  /**
    * Converts a "DatenFilm" instance into an movie for better handling
    *
    * @return the parsed movie. null if some error occured
    */
  def toMovie: Movie = {
    try {
      val releaseDateString = mov.arr(6)
      val releaseTimeString = mov.arr(7)

      val releaseDate = releaseDateParser.parse(s"$releaseTimeString $releaseDateString")

      new Movie(
        downloadUrl = new URL(mov.getUrlFuerAufloesung(DatenFilm.AUFLOESUNG_HD)),
        tvChannel = mov.arr(1),
        seriesTitle = mov.arr(2),
        episodeTitle = mov.arr(3),
        releaseDate = releaseDate,
        description = mov.arr(12),
        lengthInMinutes = mov.dauerL / 60,
        sizeInMb = mov.dateigroesseL.l.toInt
      )
    } catch {
      case _: ParseException => null
      case _: MalformedURLException => null
    }
  }
}

//noinspection SpellCheckingInspection
object DatenFilmToMovieConverter {
  implicit def datenFilm2DatenFilmToMovieConverter(value: DatenFilm): DatenFilmToMovieConverter = new DatenFilmToMovieConverter(value)
}
