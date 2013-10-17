package com.bfil.board.messages

import akka.actor.ActorRef

case class Join(username: String)
case class Joined(username: String)
case class CannotJoin(error: String)
case class Quit(username: String)

case class AddNote(text: String)
case class MoveNote(id: Int, x: Int, y: Int)

case class Move( x: Int, y: Int)

case object GetState
case object Grab
case object Grabbed
case object NotGrabbed
case object Drop

case class NoteState(id: Int, text: String, x: Int, y: Int)
case class Grab(item: ActorRef)

case object UpdateBoard
case class BoardUpdate(users: List[String], notes: List[NoteState])