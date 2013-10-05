package com.bfil.board

import com.bfil.board.actors.{ User, StickyNote }
import com.bfil.board.messages.Grab
import akka.actor.{ ActorSystem, Props }
import com.bfil.board.messages.Drop
import scala.concurrent.duration._

object Main extends App {

  val system = ActorSystem("board")

  val user = system.actorOf(Props[User], "user")
  val user2 = system.actorOf(Props[User], "user2")
  val stickyNote = system.actorOf(Props[StickyNote], "stickyNote")
  val stickyNote2 = system.actorOf(Props[StickyNote], "stickyNote2")

  sequence(0.05,
    () => user ! Grab(stickyNote),
    () => user2 ! Grab(stickyNote),
    () => user ! Drop,
    () => user2 ! Grab(stickyNote),
    () => user ! Drop,
    () => user ! Grab(stickyNote2))

  def sequence(delay: Double, tasks: (() => Unit)*) = {
    val totalTime = tasks.foldLeft(0.0)((time, task) => {
      system.scheduler.scheduleOnce(time seconds)(task())(system.dispatcher)
      time + delay
    })
    system.scheduler.scheduleOnce(totalTime seconds)(system.shutdown)(system.dispatcher)
  }
}