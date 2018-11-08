package de.dani09.moviedownloader.web

import java.security.MessageDigest
import java.util.concurrent.TimeUnit

import de.dani09.moviedownloader.data.Movie
import de.dani09.moviedownloader.web.DownloadStatus._
import de.dani09.moviedownloader.web.RemoteConnectionServlet._
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.{OnWebSocketClose, OnWebSocketConnect, OnWebSocketMessage, WebSocket}
import org.eclipse.jetty.websocket.servlet.{WebSocketServlet, WebSocketServletFactory}
import org.json.{JSONException, JSONObject}
import org.slf4j.LoggerFactory

import scala.collection.immutable.Queue

@WebSocket
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
  private lazy val md = MessageDigest.getInstance("SHA-256")
  private val methods = List[(String, JSONObject => JSONObject)](
    ("helloWorld", j => {
      val data = j.optString("data")
      val text = if (data.nonEmpty) data else "Hello World"

      new JSONObject().put("text", text)
    }),
    ("queueDownload", queueDownload)
  )
  private var connectedSessions = Set[Session]()
  private var jobQueue = Queue[(String, Movie, DownloadStatus)]()

  private def broadcast(text: String): Unit = {
    connectedSessions
      .map(_.getRemote.sendStringByFuture(text))
      .foreach(_.get(1000, TimeUnit.MILLISECONDS))
  }

  private def queueDownload(j: JSONObject): JSONObject = {
    val data = Option(j.getJSONObject("movie"))
    if (data.isEmpty)
      return new JSONObject()
        .put("status", "error")
        .put("message", "movie in json not found")

    val movie = Movie.fromJson(data.get)
    val hash = sha256(data.get.toString).substring(0, 6)

    val job = (hash, movie, QUEUED)
    jobQueue = jobQueue.enqueue(job)

    new JSONObject()
      .put("status", "success")
      .put("message", "Successfully queued DownloadJob")
      .put("place", jobQueue.length)
      .put("hash", hash)
  }

  private def sha256(s: String): String = {
    md.digest(s.getBytes)
      .map("%02x".format(_))
      .mkString
  }
}
