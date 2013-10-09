package com.bfil.board

import scala.concurrent.duration.DurationDouble

import com.bfil.board.actors.{StickyNote, User}
import com.bfil.board.messages.{Drop, Grab}

import akka.actor.{ActorSystem, Props, actorRef2Scala}

object Main extends App {

  val system = ActorSystem("board")

  val user = system.actorOf(User.props("user"), "user")
  val user2 = system.actorOf(User.props("user2"), "user2")
  val stickyNote = system.actorOf(Props[StickyNote], "stickyNote")
  val stickyNote2 = system.actorOf(Props[StickyNote], "stickyNote2")

  sequence(0.05,
    user ! Grab(stickyNote),
    user2 ! Grab(stickyNote),
    user ! Drop,
    user2 ! Grab(stickyNote),
    user ! Grab(stickyNote2),
    user ! Grab(stickyNote),
    user ! Drop)

  def sequence(delay: Double, tasks: ByName[Unit]*) = {
    val totalTime = tasks.foldLeft(0.0)((time, task) => {
      system.scheduler.scheduleOnce(time seconds)(task())(system.dispatcher)
      time + delay
    })
    system.scheduler.scheduleOnce(totalTime seconds)(system.shutdown)(system.dispatcher)
  }
  
  implicit class ByName[T]( f: => T ) {
    def apply(): T = f
  }
}
