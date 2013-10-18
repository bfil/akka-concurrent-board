package com.bfil.board.actors

import scala.concurrent.duration.DurationInt
import com.bfil.board.messages.{ AddNoteMessage, BoardUpdate, CannotJoin, ClientConnected, ClientDisconnected, JoinMessage, Joined, Quit }
import akka.actor.{ Actor, ActorLogging, Props, actorRef2Scala }
import akka.pattern.ask
import akka.util.Timeout
import com.bfil.board.messages.MoveMessage
import com.bfil.board.messages.WebSocketMessage
import org.json4s.JsonAST.JValue
import org.json4s.JsonAST.JObject
import org.json4s.JsonAST.JField
import org.json4s.JsonAST.JString
import org.json4s.jackson.JsonMethods._
import com.bfil.board.messages.GrabMessage
import com.bfil.board.messages.RemoveMessage

class WebSocketManager(broadcastToAll: AnyRef => Unit) extends Actor with ActorLogging {

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

object WebSocketManager {
  def props(broadcast: AnyRef => Unit): Props = Props(classOf[WebSocketManager], broadcast)
}