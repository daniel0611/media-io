package de.dani09.moviedownloader

import java.nio.file.Paths
import java.util.Scanner

import de.dani09.moviedownloader.config.{Config, MovieFilter}

object InteractiveMode {
  def start(): Unit = {
    println("Entering interactive mode!")
    val dl = new MovieDownloader(new Config(Paths.get("./"), 0, 0, 0, List[MovieFilter]()))

    Main.saveMovieData()
    val movies = Main.getMovies(dl)
    val s = new Scanner(System.in)

    while (true) {
      println()
      println("Creating MovieFilter")
      println("Enter an TvChannel:")
      val channel = s.nextLine()

      println("Enter an SeriesTitle Regex:")
      val seriesTitle = s.nextLine().r

      println("Creating MovieFilter")

      val filter = new MovieFilter(channel, seriesTitle)
      val matchedMovies = movies.filter(m => filter.matchesMovie(m))

      println(s"${matchedMovies.length} Movies matched entered Filter!")

      if (matchedMovies.nonEmpty) {
        println("Do you want to see them? (Y/n)")
        val answer = s.nextLine()

        if (answer.toLowerCase != "n") {
          matchedMovies.foreach(m => {
            val index = matchedMovies.indexOf(m) + 1
            val length = matchedMovies.length

            println(s"[$index/$length]")
            m.printInfo()
          })

          println("Do you want to download one? (0 or empty String if no else id)")
          println("Be sure to be in the download directory as it will download into the current folder")
          val idString = s.nextLine()

          if (idString.length != 0 && idString.forall(_.isDigit)) {
            // is number
            val id = idString.toInt
            val allowedRange = 1 to matchedMovies.length

            if (allowedRange.contains(id)) {
              dl.downloadMovie(matchedMovies(id - 1)) // TODO get Path from somewhere and not download into "./"
            }
          }
        }
      } else {
        println("Couldn't find any movies that matched this Filter")
      }
    }
  }
}
