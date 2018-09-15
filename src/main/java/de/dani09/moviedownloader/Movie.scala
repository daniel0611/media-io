package de.dani09.moviedownloader

import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.{Calendar, Date}

import de.dani09.http.Http
import org.json.JSONObject

class Movie(val downloadUrl: URL,
            val tvChannel: String,
            val seriesTitle: String,
            val episodeTitle: String,
            val releaseDate: Date,
            val description: String,
            val lengthInMinutes: Long,
            var sizeInMb: Int
           ) {

  def printInfo(withEmptyLineAtEnd: Boolean = true): Unit = {
    println(s"DownloadUrl:\t$downloadUrl")
    println(s"TvStation:\t$tvChannel")
    println(s"SeriesTitle:\t$seriesTitle")
    println(s"EpisodeTitle:\t$episodeTitle")
    println(s"ReleaseDate:\t$releaseDate")
    println(s"Length:\t\t$lengthInMinutes Minutes")
    println(s"Size:\t\t$sizeInMb Mb")

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

  private def getFileExtension(url: URL): String = url.getPath
    .split("/")
    .lastOption
    .getOrElse(".mp4")
    .split("\\.")
    .last

  def exists(): Boolean = {
    val response = Http.head(downloadUrl.toString)
      .handleRedirects(10)
      .execute()

    val exists = (200 to 299).contains(response.getResponseCode)

    if (exists) {
      val size = response.getContentLength / (1024 * 1024)
      sizeInMb = size.toInt // Updating to more accurate size than the Movie List provides
    }

    exists
  }

  def toJson: JSONObject = new JSONObject()
    .put("downloadUrl", downloadUrl.toString)
    .put("tvChannel", tvChannel)
    .put("seriesTitle", seriesTitle)
    .put("episodeTitle", episodeTitle)
    .put("releaseDate", releaseDate.getTime)
    .put("description", description)
    .put("length", lengthInMinutes)
    .put("size", sizeInMb)
}

object Movie {
  def fromJson(j: JSONObject): Movie = new Movie(
    downloadUrl = new URL(j.getString("downloadUrl")),
    tvChannel = j.getString("tvChannel"),
    seriesTitle = j.getString("seriesTitle"),
    episodeTitle = j.getString("episodeTitle"),
    releaseDate = new Date(j.getLong("releaseDate")),
    description = j.getString("description"),
    lengthInMinutes = j.getLong("length"),
    sizeInMb = j.getInt("size")
  )
}