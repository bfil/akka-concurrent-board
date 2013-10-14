package com.bfil.board

import scala.concurrent.duration.DurationDouble
import com.bfil.board.actors.Board
import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorSystem, Props, actorRef2Scala }
import com.bfil.board.servers.WebSocketServer

object Main extends App {

  val system = ActorSystem("concurrent-board", ConfigFactory.load())

  val board = system.actorOf(Props[Board], "board")

  sequence(0.05)

  def sequence(delay: Double, tasks: ByName[Unit]*) = {
    tasks.foldLeft(0.0)((time, task) => {
      system.scheduler.scheduleOnce(time seconds)(task())(system.dispatcher)
      time + delay
    })
  }

  implicit class ByName[T](f: => T) {
    def apply(): T = f
  }
  
  WebSocketServer(board).start
}
