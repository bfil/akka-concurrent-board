package com.bfil.board

import com.bfil.board.actors.{User, StickyNote}
import com.bfil.board.messages.Grab
import akka.actor.{ActorSystem, Props}
import com.bfil.board.messages.Drop
import scala.concurrent.duration._

object Main extends App {
  
  val system = ActorSystem("board")
  
  val user = system.actorOf(Props[User], "user")
  val user2 = system.actorOf(Props[User], "user2")
  val stickyNote = system.actorOf(Props[StickyNote], "stickyNote")
  val stickyNote2 = system.actorOf(Props[StickyNote], "stickyNote2")
  
  user ! Grab(stickyNote)
  
  system.scheduler.scheduleOnce(0.1 seconds)(user2 ! Grab(stickyNote))(system.dispatcher)
  
  system.scheduler.scheduleOnce(0.2 seconds)(user ! Drop)(system.dispatcher)
  
  system.scheduler.scheduleOnce(0.3 seconds)(user2 ! Grab(stickyNote))(system.dispatcher)
  
  system.scheduler.scheduleOnce(0.4 seconds)(user ! Grab(stickyNote2))(system.dispatcher)
  
  system.scheduler.scheduleOnce(0.5 seconds)(system.shutdown)(system.dispatcher)
}