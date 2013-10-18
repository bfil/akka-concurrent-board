package com.bfil.board.messages

import org.json4s.{jvalue2extractable, jvalue2monadic}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JString, JValue}

import com.bfil.board.messages.Board.{AddNote, GrabNote, Join, MoveNote, RemoveNote}
import com.bfil.board.messages.Note.NoteState

object WebSocket {

  trait Message[T] {
    implicit val formats = DefaultFormats

    def unapply(json: JValue)(implicit mf: Manifest[T]) = {
      if (json \ "message" == JString(messageName[T])) {
        Some(((json \ "data").extract[T]))
      } else None
    }

    def messageName[T](implicit m: Manifest[T]) =
      m.erasure.getSimpleName
  }

  case class WebSocketMessage(clientId: Int, message: JValue)

  case class ClientConnected(clientId: Int, channel: AnyRef => Unit, broadcast: AnyRef => Unit)
  case class ClientDisconnected(clientId: Int)
  case class BoardUpdate(users: List[String], notes: List[NoteState])

  case object JoinMessage extends Message[Join]
  case object AddNoteMessage extends Message[AddNote]
  case object GrabMessage extends Message[GrabNote]
  case object MoveMessage extends Message[MoveNote]
  case object RemoveMessage extends Message[RemoveNote]
}