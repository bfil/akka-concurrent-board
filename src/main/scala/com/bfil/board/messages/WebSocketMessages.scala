package com.bfil.board.messages

import org.json4s.{jvalue2extractable, jvalue2monadic}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.{JString, JValue}

trait Message[T] {
  implicit val formats = DefaultFormats
  
  def unapply(m : WebSocketMessage)(implicit mf: Manifest[T]) = {
    val json = m.message
    implicit val clientId = m.clientId
    if(json \ "message" == JString(messageName[T]))
    	Some(((json \ "data").extract[T]))
    else None
  }
  
  def messageName[T](implicit m: Manifest[T]) =
    m.erasure.getSimpleName
}

case class WebSocketMessage(message: JValue)(implicit _clientId: Int) {
  val clientId = _clientId
}

case class ClientConnected(clientId: Int, channel: AnyRef => Unit, broadcast: AnyRef => Unit)
case class ClientDisconnected(clientId: Int)

case object JoinMessage extends Message[Join]
case object AddNoteMessage extends Message[AddNote]
case object MoveNoteMessage extends Message[MoveNote]