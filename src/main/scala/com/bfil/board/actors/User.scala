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

class User extends Actor {
  var grabbedItem: Option[ActorRef] = None
  
  implicit val timeout: Timeout = Timeout(1 second)
  
  def receive = {
    case Grab(item) => {
      println(s"${self.path} grabbing ${item.path}")
      grabbedItem.foreach(_ ! Drop)
      item ! Grab
    }
    case Grabbed(item) => {
      grabbedItem = item
      grabbedItem match {
        case Some(_) => println(s"${self.path} grabbed ${item.map(_.path)}")
        case None => println(s"${self.path} couldn't grab")
      }
    }
    case Drop => {
      println(s"${self.path} dropping ${grabbedItem.map(_.path)}")
      grabbedItem.foreach(_ ! Drop)
    }
  }
}