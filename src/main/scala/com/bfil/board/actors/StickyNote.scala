package com.bfil.board.actors

import akka.actor.Actor
import com.bfil.board.messages.Grab
import akka.actor.ActorRef
import com.bfil.board.messages.Drop
import com.bfil.board.messages.Grabbed

class StickyNote extends Actor {
  var text = ""
  var grabbedBy: Option[ActorRef] = None

  def receive = {
    case Grab => {
      grabbedBy match {
        case None =>
          grabbedBy = Some(sender); sender ! Grabbed(Some(self))
        case Some(grabbedBy) => sender ! Grabbed(None)
      }
    }
    case Drop => grabbedBy.map(grabber => if (sender.equals(grabber)) grabbedBy = None)
  }
}