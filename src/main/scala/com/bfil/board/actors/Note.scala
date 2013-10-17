package com.bfil.board.actors

import com.bfil.board.messages.{ Drop, Grab, Grabbed, NotGrabbed }
import akka.actor.{ Actor, ActorRef, Props, actorRef2Scala }
import scala.util.Random
import com.bfil.board.messages.Move
import com.bfil.board.messages.UpdateBoard
import com.bfil.board.messages.GetState
import com.bfil.board.messages.NoteState

class Note(id: Int, _text: String) extends Actor {
  var text = _text
  var x = Random.nextInt(600)
  var y = Random.nextInt(300)
  var owner: Option[ActorRef] = None

  def receive = {
    case Grab =>
      owner match {
        case None =>
          owner = Some(sender); sender ! Grabbed
        case Some(ownedBy) => sender ! NotGrabbed
      }
    case Drop => owner.map(o => if (sender.equals(o)) owner = None)
    case Move(_x, _y) =>
      x = _x
      y = _y
      context.parent ! UpdateBoard
    case GetState => 
      sender ! NoteState(id, text, x, y)
  }
}

object Note {
  def props(id: Int, text: String): Props = Props(classOf[Note], id, text)
}