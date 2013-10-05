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

class User extends Actor with ActorLogging {
  var grabbedItem: Option[ActorRef] = None
  
  def receive = {
    case Grab(item) => {
      log.info(s"grabbing ${item.path.name}")
      grabbedItem.foreach(_ ! Drop)
      item ! Grab
    }
    case Grabbed(item) => {
      grabbedItem = item
      grabbedItem match {
        case Some(item) => log.info(s"grabbed ${item.path.name}")
        case None => log.warning(s"couldn't grab the item")
      }
    }
    case Drop => {
      if(grabbedItem.isDefined) log.info(s"dropping ${grabbedItem.map(_.path.name).get}")
      else log.info(s"nothing to drop")
      grabbedItem.foreach { item =>
        item ! Drop
        grabbedItem = None
      }
    }
  }
}