package com.bfil.board

import com.bfil.board.actors.Server
import com.bfil.board.messages.Server.Start
import com.typesafe.config.ConfigFactory

import akka.actor.{ActorSystem, Props, actorRef2Scala}

object Main extends App {
  val system = ActorSystem("concurrent-board", ConfigFactory.load())
  val server = system.actorOf(Props[Server], "server")
  server ! Start
}
