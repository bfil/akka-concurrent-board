package com.bfil.board.messages

import org.json4s.{jvalue2extractable, jvalue2monadic}
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JString

import io.backchat.hookup.JsonMessage

trait Message[T] {
  implicit val formats = DefaultFormats
  
  def unapply(m : JsonMessage)(implicit mf: Manifest[T]) = {
    val json = m.content
    if(json \ "message" == JString(messageName[T]))
    	Some((json \ "data").extract[T])
    else None
  }
  
  def messageName[T](implicit m: Manifest[T]) = {
    m.erasure.getSimpleName
  }
}

case object JoinMessage extends Message[Join]
case object AddNoteMessage extends Message[AddNote]