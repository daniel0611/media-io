package de.dani09.moviedownloader.web

/**
  * Enum for reporting status when downloading on remote server
  */
object DownloadStatus extends Enumeration {
  type DownloadStatus = Value
  val QUEUED, DOWNLOADING, FINISHED = Value
}