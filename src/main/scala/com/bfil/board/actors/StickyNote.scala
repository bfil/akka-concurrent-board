package com.bfil.board.actors

import com.bfil.board.messages.{Drop, Grab, Grabbed, NotGrabbed}

import akka.actor.{Actor, ActorRef, actorRef2Scala}

class StickyNote extends Actor {
  var text = ""
  var grabbedBy: Option[ActorRef] = None

  def receive = {
    case Grab => {
      grabbedBy match {
        case None =>
          grabbedBy = Some(sender); sender ! Grabbed
        case Some(grabbedBy) => sender ! NotGrabbed
      }
    }
    case Drop => grabbedBy.map(grabber => if (sender.equals(grabber)) grabbedBy = None)
  }
}