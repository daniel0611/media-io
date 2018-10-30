package de.dani09.moviedownloader.web

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.{OnWebSocketClose, OnWebSocketConnect, OnWebSocketMessage, WebSocket}
import org.eclipse.jetty.websocket.servlet.{WebSocketServlet, WebSocketServletFactory}

@WebSocket
class RemoteConnectionServlet extends WebSocketServlet{
  private var connectedSessions = Set[Session]()

  override def configure(webSocketServletFactory: WebSocketServletFactory): Unit = {
    webSocketServletFactory.register(getClass)
  }

  @OnWebSocketConnect
  def onConnect(session:Session): Unit ={
    connectedSessions += session
    println(s"Client ${session.getRemoteAddress} connected")
  }

  @OnWebSocketMessage
  def onMessage(session:Session, txt:String): Unit ={
    println(s"Received $txt")
    session.getRemote.sendString(txt)
  }

  @OnWebSocketClose
  def onDisconnect(session: Session, i:Int, s:String): Unit ={
    connectedSessions = connectedSessions.filter(_.isOpen) - session
    println(s"Client ${session.getRemoteAddress} disconnected. i: $i; s: $s")
  }
}
