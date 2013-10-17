package com.bfil.board.actors

import scala.concurrent.duration.DurationInt
import com.bfil.board.messages.{ AddNoteMessage, BoardUpdate, CannotJoin, ClientConnected, ClientDisconnected, JoinMessage, Joined, Quit }
import akka.actor.{ Actor, ActorLogging, Props, actorRef2Scala }
import akka.pattern.ask
import akka.util.Timeout
import com.bfil.board.messages.MoveNoteMessage

class WebSocketManager(broadcastToAll: AnyRef => Unit) extends Actor with ActorLogging {

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(1 second)

  var connections = Map.empty[Int, (AnyRef => Unit, AnyRef => Unit)]
  var usernames = Map.empty[Int, String]

  val board = context.actorOf(Props[Board], "board")

  def send(clientId: Int, message: AnyRef) =
    connections.get(clientId).foreach { case (channel, broadcast) => channel(message) }

  def broadcast(clientId: Int, message: AnyRef) =
    connections.get(clientId).foreach { case (channel, broadcast) => broadcast(message) }
  
  def withUsername(clientId: Int)(f: String => Unit) {
    usernames.get(clientId).foreach(f)
  }

  def receive = {

    case ClientConnected(clientId, channel, broadcast) =>
      connections += clientId -> (channel, broadcast)

    case ClientDisconnected(clientId) =>
       withUsername(clientId) { username =>
        usernames -= clientId
        board ! Quit(username)
        broadcast(clientId, Quit(username))
      }

    case (clientId: Int, text: String) =>
      println("Received text message \"" + text + "\" from " + clientId)

    case m @ JoinMessage(message) =>
      board ? message map {
        case joined @ Joined(username) =>
          usernames += m.clientId -> username
          broadcastToAll(joined)
        case e @ CannotJoin(error) =>
          send(m.clientId, e)
      }

    case m @ AddNoteMessage(message) =>
      board ! message

    case MoveNoteMessage(message) =>
      board ! message

    case u @ BoardUpdate(users, notes) => broadcastToAll(u)
  }
}

object WebSocketManager {
  def props(broadcast: AnyRef => Unit): Props = Props(classOf[WebSocketManager], broadcast)
}