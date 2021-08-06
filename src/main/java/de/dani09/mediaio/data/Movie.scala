package de.dani09.mediaio.data

import java.net.URL
import java.nio.file.{Path, Paths}
import java.util.{Calendar, Date}

import de.dani09.http.Http
import org.json.JSONObject

case class Movie(downloadUrl: URL,
                 tvChannel: String,
                 seriesTitle: String,
                 episodeTitle: String,
                 releaseDate: Date,
                 description: String,
                 lengthInMinutes: Long,
                 var sizeInMB: Int,
                 var groupBy: MovieGrouping.Value = MovieGrouping.NONE
                ) {

  def printInfo(withEmptyLineAtEnd: Boolean = true): Unit = {
    def withSpaces(text: String) = text.padTo(15, ' ')

    println(s"${withSpaces("DownloadUrl:")}$downloadUrl")
    println(s"${withSpaces("TvStation:")}$tvChannel")
    println(s"${withSpaces("SeriesTitle:")}$seriesTitle")
    println(s"${withSpaces("EpisodeTitle:")}$episodeTitle")
    println(s"${withSpaces("ReleaseDate:")}$releaseDate")
    println(s"${withSpaces("Length:")}$lengthInMinutes minutes")
    println(s"${withSpaces("Size:")}$sizeInMB MB")
    println(s"${withSpaces("GroupBy:")}${groupBy.toString.toLowerCase}")

    if (withEmptyLineAtEnd) println()
  }

  def getSavePath(downloadDirectory: Path): Path = {
    Paths.get(downloadDirectory.toString, getRelativeSavePath.toString)
  }

  def getRelativeSavePath: Path = {
    val calendar = Calendar.getInstance()
    calendar.setTime(releaseDate)

    val dateString = s"${calendar.get(Calendar.DAY_OF_MONTH)}.${calendar.get(Calendar.MONTH) + 1}.${calendar.get(Calendar.YEAR)}"
    val fileExtension = getFileExtension(downloadUrl)

    val title = seriesTitle.replaceAll("/", "_").replaceAll(":", ".")
    val episode = episodeTitle.replaceAll("/", "_").replaceAll(":", ".")

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1

    val relPath = groupBy match {
      case MovieGrouping.YEAR =>
        s"$tvChannel/$title/$year/$episode-$dateString.$fileExtension"
      case MovieGrouping.HALF_YEAR =>
        val yearStr = if (month < 6)
          s"${year - 1},$year"
        else
          s"$year,${year + 1}"

        s"$tvChannel/$title/$yearStr/$episode-$dateString.$fileExtension"
      case _ => s"$tvChannel/$title/$episode-$dateString.$fileExtension"
    }

    Paths.get(relPath)
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
      val size = response.getContentLength / (1000 * 1000)
      sizeInMB = size.toInt // Updating to more accurate size than the movie list provides
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
    .put("size", sizeInMB)
    .put("groupBy", groupBy.toString)
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
    sizeInMB = j.getInt("size"),
    groupBy = MovieGrouping.parse(j.optString("groupBy", ""))
  )
}
