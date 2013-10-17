package com.bfil.board

import com.bfil.board.actors.WebSocketServer
import com.bfil.board.messages.Start
import com.typesafe.config.ConfigFactory

import akka.actor.{ActorSystem, Props, actorRef2Scala}

object Main extends App {

  val system = ActorSystem("concurrent-board", ConfigFactory.load())
  val webSocketServer = system.actorOf(Props[WebSocketServer], "server")
  webSocketServer ! Start
  
}
