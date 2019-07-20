package de.dani09.mediaio.data

import de.dani09.http.HttpProgressListener
import me.tongfei.progressbar.{ProgressBar, ProgressBarBuilder}

import scala.language.implicitConversions

class ProgressBarBuilder2HttpProgressListener(pbb: ProgressBarBuilder) {
  def toHttpProgressListener: HttpProgressListener = new HttpProgressListener {
    var pb: ProgressBar = _

    override def onStart(l: Long): Unit = {
      pb = pbb
        .setInitialMax(l)
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
}

object ProgressBarBuilder2HttpProgressListener {
  implicit def progressBarBuilder2HttpProgressListener(pbb: ProgressBarBuilder): ProgressBarBuilder2HttpProgressListener = new ProgressBarBuilder2HttpProgressListener(pbb)
}
