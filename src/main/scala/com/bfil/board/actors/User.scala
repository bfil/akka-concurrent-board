package com.bfil.board.actors

import com.bfil.board.messages.{Drop, Grab, Grabbed, NotGrabbed}
import akka.actor.{Actor, ActorLogging, ActorRef, actorRef2Scala}
import akka.actor.Props

class User(username: String) extends Actor with ActorLogging {
  var grabbedItem: Option[ActorRef] = None
  
  def dropItem = {
    if(grabbedItem.isDefined) log.info(s"dropping ${grabbedItem.map(_.path.name).get}")
    else log.info(s"nothing to drop")
    grabbedItem.foreach(_ ! Drop)
    grabbedItem = None
  }
  
  def receive = {
    case Grab(item) => {
      log.info(s"grabbing ${item.path.name}")
      dropItem
      item ! Grab
      context.become(grabbing(item))
    }
    case Drop => {
      dropItem
    }
  }
  
  def grabbing(item: ActorRef): Receive = {
    case Grabbed => {
      grabbedItem = Some(item)
      log.info(s"grabbed ${item.path.name}")
      context.unbecome()
    }
    case NotGrabbed => {
      log.info(s"could not grab ${item.path.name}")
      context.unbecome()
    }
  }
}

object User {
  def props(name: String): Props = Props(classOf[User], name)
}