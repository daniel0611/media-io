package de.dani09.moviedownloader.web

object DownloadStatus extends Enumeration {
  type DownloadStatus = Value
  val QUEUED, DOWNLOADING, FINISHED = Value
}