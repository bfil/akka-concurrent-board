package com.bfil.board.actors

import scala.concurrent.duration.DurationInt

import com.bfil.board.messages.{AddNoteMessage, BoardUpdate, CannotJoin, ClientConnected, ClientDisconnected, JoinMessage, Joined, NoteAdded, Quit}

import akka.actor.{Actor, ActorLogging, Props, actorRef2Scala}
import akka.pattern.ask
import akka.util.Timeout

class WebSocketManager(broadcastToAll: AnyRef => Unit) extends Actor with ActorLogging {

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global

  var connections = Map.empty[Int, (AnyRef => Unit, AnyRef => Unit)]
  var usernames = Map.empty[Int, String]

  val board = context.actorOf(Props[Board], "board")

  def send(id: Int, message: AnyRef) =
    connections.get(id) match {
      case Some((channel, broadcast)) => channel(message)
      case None =>
    }
  
  def broadcast(id: Int, message: AnyRef) =
    connections.get(id) match {
      case Some((channel, broadcast)) => broadcast(message)
      case None =>
    }

  def receive = {

    case ClientConnected(id, channel, broadcast) =>
      connections += (id -> (channel, broadcast))

    case ClientDisconnected(id) =>
      usernames.get(id) match {
        case Some(username) =>
          usernames -= id
          board ! Quit(username)
          broadcast(id, Quit(username))
        case None =>
      }
      
    case (id: Int, text: String) =>
      println("Received text message \"" + text + "\" from " + id)

    case JoinMessage(id, message) =>
      val username = message.username

      implicit val timeout = Timeout(1 second)
      board ? message map {
        case joined @ Joined(username) =>
          usernames += (id -> username)
          broadcastToAll(joined)
        case e @ CannotJoin(error) =>
          send(id, e)
      }

    case AddNoteMessage(id, message) =>
      board ! message
      usernames.get(id) match {
        case Some(username) =>
          broadcastToAll(NoteAdded(username))
        case None =>
      }
      
    case u @ BoardUpdate(_,_,_) => broadcastToAll(u)
  }
}

object WebSocketManager {
  def props(broadcast: AnyRef => Unit): Props = Props(classOf[WebSocketManager], broadcast)
}