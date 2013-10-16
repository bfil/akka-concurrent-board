package com.bfil.board.actors

import scala.concurrent.duration.DurationInt
import com.bfil.board.messages.{ AddNoteMessage, BoardUpdate, CannotJoin, ClientConnected, ClientDisconnected, JoinMessage, Joined, NoteAdded, Quit }
import akka.actor.{ Actor, ActorLogging, Props, actorRef2Scala }
import akka.pattern.ask
import akka.util.Timeout
import com.bfil.board.messages.MoveNoteMessage

class WebSocketManager(broadcastToAll: AnyRef => Unit) extends Actor with ActorLogging {

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  var connections = Map.empty[Int, (AnyRef => Unit, AnyRef => Unit)]
  var usernames = Map.empty[Int, String]

  val board = context.actorOf(Props[Board], "board")

  def send(clientId: Int, message: AnyRef) =
    connections.get(clientId).foreach { case (channel, broadcast) => channel(message) }

  def broadcast(clientId: Int, message: AnyRef) =
    connections.get(clientId).foreach { case (channel, broadcast) => broadcast(message) }

  def receive = {

    case ClientConnected(clientId, channel, broadcast) =>
      connections += (clientId -> (channel, broadcast))

    case ClientDisconnected(clientId) =>
      usernames.get(clientId).foreach { username =>
        usernames -= clientId
        board ! Quit(username)
        broadcast(clientId, Quit(username))
      }

    case (clientId: Int, text: String) =>
      println("Received text message \"" + text + "\" from " + clientId)

    case JoinMessage(clientId, message) =>
      val username = message.username

      implicit val timeout = Timeout(1 second)
      board ? message map {
        case joined @ Joined(username) =>
          usernames += (clientId -> username)
          broadcastToAll(joined)
        case e @ CannotJoin(error) =>
          send(clientId, e)
      }

    case AddNoteMessage(clientId, message) =>
      board ! message
      usernames.get(clientId).foreach(username => broadcastToAll(NoteAdded(username)))

    case MoveNoteMessage(clientId, message) =>
      board ! message

    case u @ BoardUpdate(_, _, _) => broadcastToAll(u)
  }
}

object WebSocketManager {
  def props(broadcast: AnyRef => Unit): Props = Props(classOf[WebSocketManager], broadcast)
}