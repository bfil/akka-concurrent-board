package com.bfil.board.servers

import io.backchat.hookup._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{ read, write }
import akka.actor.ActorRef
import akka.pattern.ask
import com.bfil.board.messages.Join
import akka.util.Timeout
import scala.concurrent.duration._
import com.bfil.board.messages.Joined
import com.bfil.board.messages.CannotJoin
import com.bfil.board.messages.Quit
import com.bfil.board.messages.AddNote
import com.bfil.board.messages.JoinMessage
import com.bfil.board.messages.AddNoteMessage
import com.bfil.board.messages.NoteAdded

case class WebSocketServer(board: ActorRef) {

  implicit def stringToTextMessage(s: String) = TextMessage(s)
  implicit val formats = DefaultFormats
  implicit val timeout = Timeout(1 second)

  val get = (HookupServer(8125) {
    new HookupServerClient {

      var usernames = Map.empty[Int, String]

      def receive = {
        case Connected =>
          println(s"Client connected: $id")

        case Disconnected(_) =>
          println(s"Client disconnected: $id")
          usernames.get(id) match {
            case Some(username) => {
              usernames -= id
              board ! Quit(username)
              broadcast(toJson(Quit(username)))
            }
            case None =>
          }

        case m @ Error(ex) =>
          System.err.println(s"Received an error: $m")
          ex foreach { _.printStackTrace(System.err) }

        case m: TextMessage =>
          println(m)
          send(m)

        case JoinMessage(message) =>
          val username = message.username
          board ? message map {
            case joined @ Joined(username) =>
              usernames += (id -> username)
              send(toJson(joined))
              broadcast(toJson(joined))
            case e @ CannotJoin(error) =>
              send(toJson(e))
          }

        case AddNoteMessage(message) =>
          board ! message
          usernames.get(id) match {
            case Some(username) => {
              send(toJson(NoteAdded(username)))
              broadcast(toJson(NoteAdded(username)))
            }
            case None =>
          }

        case m: JsonMessage =>
          println("JsonMessage(" + pretty(render(m.content)) + ")")
          send(m)
      }
    }
  })

  def toJson(message: AnyRef) =
    "{\"message\":\"" + message.getClass.getSimpleName().replace("$", "") + "\",\"data\":" + write(message) + "}"
}