package de.dani09.moviedownloader

import java.net.URL


class Movie(val downloadUrl: URL,
            val tvChannel: String,
            val seriesTitle: String,
            val episodeTitle: String,
            val releaseDate: String,
            val releaseTime: String,
            val description: String,
            val lengthInMinutes: Long,
            val sizeInMb: Int
           ) {

  def printInfo(withEmptyLineAtEnd: Boolean = true): Unit = {
    println(s"DownloadUrl\t\t$downloadUrl")
    println(s"Station\t\t\t$tvChannel")
    println(s"SeriesTitle\t\t$seriesTitle")
    println(s"EpisodeTitle\t$episodeTitle")
    println(s"ReleaseDate\t\t$releaseDate")
    println(s"ReleaseTime\t\t$releaseTime")
    println(s"Description\t\t$description")
    println(s"Length\t\t\t$lengthInMinutes")
    println(s"SizeInMb\t\t$sizeInMb")

    if (withEmptyLineAtEnd) println()
  }
}