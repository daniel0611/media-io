package de.dani09.moviedownloader.config

import java.io.{File, FileWriter}
import java.nio.file.Paths

import de.dani09.moviedownloader.Movie
import org.json.{JSONArray, JSONException}

import scala.collection.mutable.ListBuffer
import scala.io.Source

class DownloadedMovies(private val movies: ListBuffer[Movie]) {

  def this(movies: List[Movie]) = this(movies.to[ListBuffer])

  def serialize(config: Config): Unit = {
    val file = DownloadedMovies.getListFile(config)

    val jsonString = movies
      .map(_.toJson)
      .foldLeft(new JSONArray())((arr, x) => arr.put(x))
      .toString

    val fw = new FileWriter(file)
    fw.write(jsonString)
    fw.close()
  }

  def addMovie(m: Movie): Unit = movies += m

  def getMovies: ListBuffer[Movie] = movies
}

object DownloadedMovies {
  def deserialize(config: Config): DownloadedMovies = {
    val file = getListFile(config)
    if (!file.exists()) return null

    val text = Source.fromFile(file).getLines().mkString("")
    val arr = new JSONArray(text)

    val movies = (for (i <- 0 until arr.length()) yield i)
      .par
      .map(index => arr.getJSONObject(index))
      .map(j => {
        try {
          Movie.fromJson(j)
        } catch {
          case _: JSONException => null
        }
      })
      .filter(_ != null)
      .toList

    new DownloadedMovies(movies)
  }

  private def getListFile(config: Config): File = Paths
    .get(config.downloadDirectory.toString, "./downloaded-movies.json")
    .toFile
}