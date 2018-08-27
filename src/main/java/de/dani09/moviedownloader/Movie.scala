package de.dani09.moviedownloader

import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.{Calendar, Date}

import de.dani09.http.Http

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

  def getSavePath(downloadDirectory: Path): Path = {
    val calendar = Calendar.getInstance()
    calendar.setTime(releaseDate)

    val dateString = s"${calendar.get(Calendar.DAY_OF_MONTH)}.${calendar.get(Calendar.MONTH)}.${calendar.get(Calendar.YEAR)}"
    val fileExtension = getFileExtension(downloadUrl)

    val title = seriesTitle.replaceAll("/", "_").replaceAll(":", ".")
    val episode = episodeTitle.replaceAll("/", "_").replaceAll(":", ".")

    val pathString = s"$downloadDirectory/$tvChannel/$title/$episode-$dateString.$fileExtension"
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

  def exists(): Boolean = {
    val responseCode = Http.head(downloadUrl.toString)
      .handleRedirects(10)
      .execute()
      .getResponseCode

    (200 to 299).contains(responseCode)
  }
}