package com.bfil.board.servers

import scala.concurrent.duration.DurationInt

import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.{pretty, render}
import org.json4s.jackson.Serialization.write

import com.bfil.board.messages.{AddNoteMessage, CannotJoin, JoinMessage, Joined, NoteAdded, Quit}

import akka.actor.{ActorRef, actorRef2Scala}
import akka.pattern.ask
import akka.util.Timeout
import io.backchat.hookup.{Connected, Disconnected, Error, HookupServer, HookupServerClient, JsonMessage, OutboundMessage, TextMessage}

object WebSocketServer {

  def apply(board: ActorRef) = (HookupServer(8125) {
    new HookupServerClient {

      var usernames = Map.empty[Int, String]

      def receive = {
        case Connected =>
          println(s"Client connected: $id")

        case Disconnected(_) =>
          println(s"Client disconnected: $id")
          usernames.get(id) match {
            case Some(username) =>
              usernames -= id
              board ! Quit(username)
              broadcast(Quit(username))
            case None =>
          }

        case JoinMessage(message) =>
          val username = message.username
          implicit val timeout = Timeout(1 second)
          board ? message map {
            case joined @ Joined(username) =>
              usernames += (id -> username)
              send(joined)
              broadcast(joined)
            case e @ CannotJoin(error) =>
              send(e)
          }

        case AddNoteMessage(message) =>
          board ! message
          usernames.get(id) match {
            case Some(username) =>
              send(NoteAdded(username))
              broadcast(NoteAdded(username))
            case None =>
          }

        case error @ Error(ex) =>
          System.err.println(s"Received an error: $error")
          ex foreach { _.printStackTrace(System.err) }

        case text: TextMessage =>
          println(text)
          send(text)

        case json: JsonMessage =>
          println("JsonMessage(" + pretty(render(json.content)) + ")")
          send(json)
      }
    }
  })

  implicit def stringToTextMessage(s: String) = TextMessage(s)
  implicit def toJson(message: AnyRef): OutboundMessage = {
    implicit val formats = DefaultFormats
    "{\"message\":\"" + message.getClass.getSimpleName.replace("$", "") + "\",\"data\":" + write(message) + "}"
  }
}