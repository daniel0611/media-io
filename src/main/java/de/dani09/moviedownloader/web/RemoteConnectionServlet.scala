package de.dani09.moviedownloader.web

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.concurrent.{Executors, ScheduledThreadPoolExecutor, TimeUnit}

import de.dani09.http.HttpProgressListener
import de.dani09.moviedownloader.MovieDownloaderUtil
import de.dani09.moviedownloader.config.{Config, DownloadedMovies}
import de.dani09.moviedownloader.data.Movie
import de.dani09.moviedownloader.web.DownloadStatus._
import de.dani09.moviedownloader.web.RemoteConnectionServlet._
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.{OnWebSocketClose, OnWebSocketConnect, OnWebSocketMessage, WebSocket}
import org.eclipse.jetty.websocket.servlet.{WebSocketServlet, WebSocketServletFactory}
import org.json.{JSONException, JSONObject}
import org.slf4j.LoggerFactory

import scala.collection.immutable.Queue
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random

@WebSocket(maxIdleTime = 10000)
class RemoteConnectionServlet extends WebSocketServlet {
  private val logger = LoggerFactory.getLogger(getClass)

  override def configure(webSocketServletFactory: WebSocketServletFactory): Unit = {
    webSocketServletFactory.register(getClass)
  }

  @OnWebSocketConnect
  def onConnect(session: Session): Unit = {
    connectedSessions += session
    logger.info(s"Client ${session.getRemoteAddress} connected")
    logger.info(s"${connectedSessions.size} sockets currently connected")
  }

  @OnWebSocketMessage
  def onMessage(session: Session, txt: String): Unit = {
    val request = parseMessage(txt)
    if (request._2 == null) {
      session.getRemote
        .sendString(
          new JSONObject()
            .put("status", "error")
            .put("message", s"Could not parse message")
            .put("exception", request._1)
            .toString
        )
      return
    }

    val m = methods.find(_._1.equalsIgnoreCase(request._1))

    if (m.isDefined) {
      val result = m.get._2(request._2)

      if (result != null)
        session.getRemote.sendString(result.toString)
    } else {
      session.getRemote
        .sendString(
          new JSONObject()
            .put("status", "error")
            .put("message", s"Could not find method")
            .put("exception", s"Method with name ${request._1} could not be found")
            .toString
        )
    }
  }

  private def parseMessage(msg: String): (String, JSONObject) = {
    try {
      val json = new JSONObject(msg)
      val method = json.getString("method")

      (method, json)
    } catch {
      case e: JSONException => (e.toString, null)
      case e: Throwable =>
        logger.error(s"Unknown error while parsing WebSocket message: $e")
        null
    }
  }

  @OnWebSocketClose
  def onDisconnect(session: Session, i: Int, s: String): Unit = {
    connectedSessions = connectedSessions.filter(_.isOpen) - session
    logger.info(s"Client ${session.getRemoteAddress} disconnected. i: $i; s: $s")
    logger.info(s"${connectedSessions.size} sockets currently connected")
  }
}

object RemoteConnectionServlet {
  private val jobExecutor = Executors.newFixedThreadPool(1)
  private val pingExecutor = new ScheduledThreadPoolExecutor(1)
  private implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(jobExecutor)
  private var config: Config = _
  private val logger = LoggerFactory.getLogger(getClass)
  private lazy val md = MessageDigest.getInstance("SHA-256")
  private val methods = List[(String, JSONObject => JSONObject)](
    ("queueDownload", queueDownload),
    ("getJobStatus", jobStatus)
  )
  private var connectedSessions = Set[Session]()
  private var jobQueue = Queue[(String, Movie, DownloadStatus, Future[Unit])]()

  def initConfig(c: Config): Unit = config = c

  startPinging()

  private def startPinging(): Unit = {
    val run = new Runnable {
      override def run(): Unit = {
        val data = Random.alphanumeric.take(10).mkString
        val payload = ByteBuffer.wrap(data.getBytes)
        connectedSessions.foreach(_.getRemote.sendPing(payload))
      }
    }
    pingExecutor.scheduleAtFixedRate(run, 2, 5, TimeUnit.SECONDS)
    logger.info("Pinging each connected Socket every 5 seconds")
  }

  private def queueDownload(j: JSONObject): JSONObject = {
    val data = Option(j.getJSONObject("movie"))
    if (data.isEmpty)
      return new JSONObject()
        .put("status", "error")
        .put("message", "movie in json not found")

    val movie = Movie.fromJson(data.get)
    val hash = sha256(data.get.toString).substring(0, 7)

    val job = (hash, movie, QUEUED, getJobFuture(hash, config))
    jobQueue = jobQueue.enqueue(job).distinct

    logger.info(s"Download Job with hash $hash was enqueued")

    new JSONObject()
      .put("status", "success")
      .put("message", "Successfully queued DownloadJob")
      .put("place", jobQueue.length)
      .put("hash", hash)
  }

  private def broadcast(text: String): Unit = {
    connectedSessions
      .map(_.getRemote.sendStringByFuture(text))
      .foreach(_.get(1000, TimeUnit.MILLISECONDS))
  }

  private def broadcast(json: JSONObject): Unit = broadcast(json.toString)

  private def jobStatus(j: JSONObject): JSONObject = {
    val hash = j.optString("hash")
    if (hash.isEmpty)
      return new JSONObject()
        .put("status", "error")
        .put("message", "Hash not found in json")

    if (hash.length != 7)
      return new JSONObject()
        .put("status", "error")
        .put("message", "hash has the wrong length")

    val job = jobQueue.find(_._1 == hash)
    if (job.isEmpty)
      return new JSONObject()
        .put("status", "error")
        .put("message", "Job not enqueued")

    val json = new JSONObject()
      .put("hash", job.get._1)
      .put("movie", job.get._2.toJson)
      .put("jobStatus", job.get._3.toString)

    logger.info(s"Status of job with hash $hash has been requested and been broadcast")

    broadcast(json.put("status", "update"))
    json.put("status", "success")
  }

  private def getJobFuture(jobHash: String, config: Config): Future[Unit] = Future[Unit] {
    val job = jobQueue.find(_._1 == jobHash).get.copy(_3 = DOWNLOADING) // get job with updated status
    jobQueue = jobQueue.map(j => if (j._1 == jobHash) job else j) // update job in queue
    logger.info(s"Download Job with hash $jobHash started")

    val mdu = new MovieDownloaderUtil(config, new PrintStream(new ByteArrayOutputStream()))

    var timesProgressCalled = 0
    mdu.downloadMovie(job._2, new HttpProgressListener {
      override def onStart(l: Long): Unit = broadcastJobStatus(job, 0, l)

      override def onProgress(v: Double): Unit = {}

      override def onProgress(done: Long, max: Long): Unit = {
        timesProgressCalled += 1

        if (timesProgressCalled % 35 == 0)
          broadcastJobStatus(job, done, max)
      }

      override def onFinish(): Unit = {}
    })

    val dm = DownloadedMovies.deserialize(config)
    dm.addMovie(job._2)
    dm.serialize(config)

    jobQueue = jobQueue.map(j => if (j._1 == jobHash) job.copy(_3 = FINISHED) else j)
    logger.info(s"Download Job with hash $jobHash finished")
    broadcastJobStatus(job.copy(_3 = FINISHED))
  }

  private def broadcastJobStatus(job: (String, Movie, DownloadStatus, Future[Unit]), progress: Long = 0, maxProgress: Long = 0): Unit = {
    val base = new JSONObject()
      .put("status", "update")
      .put("hash", job._1)
      .put("jobStatus", job._3.toString)

    broadcast(
      if (maxProgress == 0)
        base
      else
        base.put("progress", progress)
          .put("maxProgress", maxProgress)
    )
  }

  private def sha256(s: String): String = {
    md.digest(s.getBytes)
      .map("%02x".format(_))
      .mkString
  }
}
