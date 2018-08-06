package de.dani09.moviedownloader

import java.net.URL
import java.util.Date


class Movie(val downloadUrl: URL,
            val tvChannel: String,
            val seriesTitle: String,
            val episodeTitle: String,
            val releaseDate: Date,
            val description: String,
            val lengthInMinutes: Long,
            val sizeInMb: Int
           ) {

  def printInfo(withEmptyLineAtEnd: Boolean = true): Unit = {
    println(s"DownloadUrl\t\t$downloadUrl")
    println(s"Station\t\t\t$tvChannel")
    println(s"SeriesTitle\t\t$seriesTitle")
    println(s"EpisodeTitle\t\t$episodeTitle") // TODO Broken sometimes 2 tabs sometimes only one needed
    println(s"ReleaseDate\t\t$releaseDate")
    println(s"Description\t\t$description")
    println(s"Length\t\t\t$lengthInMinutes")
    println(s"SizeInMb\t\t$sizeInMb")

    if (withEmptyLineAtEnd) println()
  }
}