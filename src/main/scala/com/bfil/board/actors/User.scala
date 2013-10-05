package com.bfil.board.actors

import akka.actor.Actor
import akka.actor.ActorRef
import com.bfil.board.messages.Grab
import com.bfil.board.messages.Drop
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import com.bfil.board.messages.Grabbed
import akka.actor.ActorLogging
import com.bfil.board.messages.NotGrabbed

class User extends Actor with ActorLogging {
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
      context.become(receive)
    }
    case NotGrabbed => {
      log.info(s"could not grab ${item.path.name}")
      context.become(receive)
    }
  }
}