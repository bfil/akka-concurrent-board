package com.bfil.board.messages

import akka.actor.ActorRef
import org.json4s._
import org.json4s.jackson.JsonMethods._
import io.backchat.hookup.JsonMessage

case class Join(username: String)
case class Joined(username: String)
case class CannotJoin(error: String)
case class Quit(username: String)

case class AddNote
case class NoteAdded(username: String)

case object Grab
case object Grabbed
case object NotGrabbed
case object Drop

case class Grab(item: ActorRef)

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

object JoinMessage extends Message[Join]
object AddNoteMessage extends Message[AddNote]