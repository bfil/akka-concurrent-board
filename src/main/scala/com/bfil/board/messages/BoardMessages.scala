package com.bfil.board.messages

import akka.actor.ActorRef

case class Join(username: String)
case class Joined(username: String)
case class CannotJoin(error: String)
case class Quit(username: String)

case class AddNote(text: String)
case class NoteAdded(username: String)

case object Grab
case object Grabbed
case object NotGrabbed
case object Drop

case class Grab(item: ActorRef)

case class BoardUpdate(notes: List[Int], positions: List[List[Int]], texts: List[String])