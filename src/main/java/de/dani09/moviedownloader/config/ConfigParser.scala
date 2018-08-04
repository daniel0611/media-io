package de.dani09.moviedownloader.config

import java.nio.file.{Files, Paths}

import org.json.JSONObject

import scala.collection.JavaConverters._

object ConfigParser {
  def parseConfig(path: String): Config = {
    val configString = Files.readAllLines(Paths.get(path)).asScala.mkString("")
    val configJson = new JSONObject(configString)

    // Get Filters
    val filterArray = configJson.getJSONArray("filters")
    val filters = (for (i <- 0 until filterArray.length()) yield i)
      .map(index => filterArray.getJSONObject(index))
      .map(f => new MovieFilter(f.getString("tvChannel"), f.getString("seriesTitle").r))
      .toList

    new Config(
      downloadDirectory = Paths.get(configJson.getString("downloadDirectory")),
      minimumSize = configJson.getInt("minimumSize"),
      minimumLength = configJson.getLong("minimumLength"),
      maxDaysOld = configJson.getInt("maxDaysOld"),
      filters
    )
  }
}
