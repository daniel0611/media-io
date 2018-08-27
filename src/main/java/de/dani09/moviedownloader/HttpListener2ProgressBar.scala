package de.dani09.moviedownloader

import de.dani09.http.HttpProgressListener
import me.tongfei.progressbar.{ProgressBar, ProgressBarBuilder}

class HttpListener2ProgressBar(pbb: ProgressBarBuilder) extends HttpProgressListener {
  var pb: ProgressBar = null

  override def onStart(l: Long): Unit = {
    pb = pbb.
      setInitialMax(l)
      .build()
  }

  override def onProgress(bytesRead: Long, bytesTotal: Long): Unit = {
    pb.stepTo(bytesRead)
  }

  override def onProgress(v: Double): Unit = {}

  override def onFinish(): Unit = {
    pb.close()
  }
}
