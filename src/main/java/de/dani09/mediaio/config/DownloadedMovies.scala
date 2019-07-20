package de.dani09.mediaio.config

import java.io.{File, FileWriter}
import java.nio.file.Paths

import de.dani09.mediaio.data.Movie
import org.json.{JSONArray, JSONException}

import scala.collection.mutable.ListBuffer
import scala.io.Source

class DownloadedMovies(private val movies: ListBuffer[Movie]) {

  def this(movies: List[Movie]) = this(movies.to[ListBuffer]) // converts List to ListBuffer in order to use an List as an argument

  def this() = this(ListBuffer[Movie]()) // empty constructor

  /**
    * Serializes the DownloadedMovies list by converting it to json and writing it to the file "downloaded-movies.json"
    *
    * @param config config is needed to get the path where the file will be saved
    */
  def serialize(config: Config): Unit = {
    val file = DownloadedMovies.getDownloadedMoviesFile(config)

    val jsonString = this.toJson.toString

    val fw = new FileWriter(file)
    fw.write(jsonString)
    fw.close()
  }

  /**
    * Converts the list of DownloadedMovies into an JSONArray
    *
    * @return returns this list as an JSONArray
    */
  def toJson: JSONArray = {
    movies
      .groupBy(_.downloadUrl) // this...
      .map(_._2.head) // ... and this filters duplicates with the same download url
      .map(_.toJson) // convert Movies to json // List[Movie] -> List[JSONObject]
      .foldLeft(new JSONArray())((arr, x) => arr.put(x)) // put JSONObjects into JSONArray // List[JSONObject] -> JSONArray
  }

  /**
    * Adds the movie to the list
    *
    * @param m the movie you want to add
    */
  def addMovie(m: Movie): Unit = movies += m

  /**
    * Get all movies as an Scala ListBuffer
    *
    * @return all movies
    */
  def getMovies: ListBuffer[Movie] = movies
}

object DownloadedMovies {

  /**
    * deSerializes an DownloadedMovies list by parsing the file in which it is stored and parsing this json
    *
    * @param config config is needed to get the path where the file is located
    * @return returns an instance of DownloadedMovies with the parsed data
    */
  def deserialize(config: Config): DownloadedMovies = {
    val file = getDownloadedMoviesFile(config)
    if (!file.exists()) new DownloadedMovies()

    try {
      val source = Source.fromFile(file)
      val text = source.getLines().mkString("")
      source.close()

      val arr = new JSONArray(text)

      val movies = (for (i <- 0 until arr.length()) yield i)
        .par
        .map(index => arr.getJSONObject(index)) // get JSONObjects by index
        .map(json => { // Parse Movies with JSONObjects
        try {
          Movie.fromJson(json)
        } catch {
          case _: JSONException => null // will be removed by filter afterwards
        }
      }) // ParSeq[JSONObject] -> ParSeq[Movie]
        .filter(_ != null) // filter those out which did threw an JSONException
        .toList // ParSeq[Movies] -> List[Movies]

      new DownloadedMovies(movies)
    } catch {
      case _: Throwable =>
        println("Couldn't parse downloaded-movies.json!")
        new DownloadedMovies()
    }
  }

  /**
    * get the path where the DownloadedMovies file is located
    *
    * @param config config is needed to get the DownloadDirectory
    * @return returns the path where the file is
    */
  private def getDownloadedMoviesFile(config: Config): File = Paths
    .get(config.downloadDirectory.toString, "./downloaded-movies.json")
    .toFile
}