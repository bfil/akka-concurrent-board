package com.bfil.board.messages

import akka.actor.ActorRef

case class Join(username: String)
case class Joined(username: String)
case class CannotJoin(error: String)
case class Quit(username: String)

case class AddNote(username: String, text: String)
case class Move(username: String, id: Int, x: Int, y: Int)
case class Remove(username: String, id: Int)

case object GetState
case object GetUsername

object User {
  case class GrabNote(note: ActorRef)
  case class MoveNote(note: ActorRef, x: Int, y: Int)
  case class RemoveNote(note: ActorRef)
}

object Note {
  case object Grab
  case object Drop
  case object Grabbed
  case object NotGrabbed
  case class Move(x: Int, y: Int)
  case object Remove
}

case class NoteState(id: Int, text: String, x: Int, y: Int, owner: Option[String])
case class Grab(username: String, id: Int)

case class NoteRemoved(id: Int)
case object UpdateBoard
case class BoardUpdate(users: List[String], notes: List[NoteState])