package com.bfil.board.actors

import com.bfil.board.messages.{Drop, Grab, Grabbed, NotGrabbed}

import akka.actor.{Actor, ActorRef, actorRef2Scala}

class Note extends Actor {
  var text = ""
  var owner: Option[ActorRef] = None

  def receive = {
    case Grab => {
      owner match {
        case None =>
          owner = Some(sender); sender ! Grabbed
        case Some(ownedBy) => sender ! NotGrabbed
      }
    }
    case Drop => owner.map(o => if (sender.equals(o)) owner = None)
  }
}