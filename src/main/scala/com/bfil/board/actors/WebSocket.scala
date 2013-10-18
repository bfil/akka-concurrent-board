package com.bfil.board.actors

import scala.concurrent.duration.DurationInt

import org.json4s.JsonAST.{JObject, JString, JValue}
import org.json4s.jackson.JsonMethods.{pretty, render}
import org.json4s.jvalue2monadic

import com.bfil.board.messages.Board.{CannotJoin, Joined, Quit}
import com.bfil.board.messages.WebSocket.{AddNoteMessage, BoardUpdate, ClientConnected, ClientDisconnected, GrabMessage, JoinMessage, MoveMessage, RemoveMessage, WebSocketMessage}

import akka.actor.{Actor, ActorLogging, Props, actorRef2Scala}
import akka.pattern.ask
import akka.util.Timeout

class WebSocket(broadcastToAll: AnyRef => Unit) extends Actor with ActorLogging {

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(1 second)

  var connections = Map.empty[Int, (AnyRef => Unit, AnyRef => Unit)]
  var usernames = Map.empty[Int, String]

  val board = context.actorOf(Props[Board], "board")

  def send(message: AnyRef)(implicit clientId: Int) =
    connections.get(clientId).foreach { case (channel, broadcast) => channel(message) }

  def broadcast(message: AnyRef)(implicit clientId: Int) =
    connections.get(clientId).foreach { case (channel, broadcast) => broadcast(message) }

  def withUsername(clientId: Int)(f: String => Unit) {
    usernames.get(clientId).foreach(f)
  }

  def wsReceive(implicit clientId: Int): PartialFunction[JValue, Unit] = {

    case JoinMessage(message) =>
      board ? message map {
        case joined @ Joined(username) =>
          usernames += clientId -> username
          broadcastToAll(joined)
        case e @ CannotJoin(error) =>
          send(e)
      }

    case AddNoteMessage(message) =>
      board ! message

    case MoveMessage(message) =>
      board ! message
      
    case RemoveMessage(message) =>
      board ! message
      
    case GrabMessage(message) =>
      board ? message map {
        case x: AnyRef => send(x)
      }

    case x =>
      println(s"Unhandled WebSocketMessage: ${pretty(render(x))}")
  }

  def receive = {

    case ClientConnected(clientId, channel, broadcast) =>
      connections += clientId -> (channel, broadcast)

    case ClientDisconnected(clientId) =>
      implicit val _clientId = clientId
      withUsername(clientId) { username =>
        usernames -= clientId
        board ! Quit(username)
        broadcast(Quit(username))
      }

    case (clientId: Int, text: String) =>
      println("Received text message \"" + text + "\" from " + clientId)

    case WebSocketMessage(clientId, message) =>
      val json = usernames.get(clientId) match {
        case Some(username) =>
          val mergedData = (message \ "data") merge JObject("username" -> JString(username))
          message merge (JObject("data" -> mergedData))
        case None => message
      }
      wsReceive(clientId)(json)

    case u @ BoardUpdate(users, notes) =>
      broadcastToAll(u)
  }
}

object WebSocket {
  def props(broadcast: AnyRef => Unit): Props = Props(classOf[WebSocket], broadcast)
}