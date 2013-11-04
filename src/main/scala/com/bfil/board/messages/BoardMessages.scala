package com.bfil.board.messages

import akka.actor.ActorRef

object Board {
  case class Join(username: String)
  case class Joined(username: String)
  case class CannotJoin(error: String)
  case class Quit(username: String)
  case class AddNote(username: String, text: String)
  case class GrabNote(username: String, noteId: Int)
  case class MoveNote(username: String, noteId: Int, x: Int, y: Int)
  case class EditNote(username: String, noteId: Int, text: String)
  case class RemoveNote(username: String, noteId: Int)
  case class NoteRemoved(id: Int)
  
  case object Update
}

object User {
  case class GrabNote(note: ActorRef)
  case class MoveNote(note: ActorRef, x: Int, y: Int)
  case class EditNote(note: ActorRef, text: String)
  case class RemoveNote(note: ActorRef)
  
  case object DropNote
  case object GetUsername
}

object Note {
  case class Move(x: Int, y: Int)
  case class Edit(text: String)
  case class NoteState(id: Int, text: String, x: Int, y: Int, owner: Option[String])
  
  case object Grab
  case object Drop
  case object Grabbed
  case object NotGrabbed
  case object Remove
  case object GetState
}