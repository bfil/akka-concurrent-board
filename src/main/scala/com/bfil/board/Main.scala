package com.bfil.board

import com.bfil.board.actors.Server
import com.bfil.board.messages.Server.Start
import com.typesafe.config.ConfigFactory
import akka.actor.{ActorSystem, Props, actorRef2Scala}
import akka.io.IO
import spray.can.Http
import com.bfil.board.actors.StaticServer

object Main extends App {
  implicit val system = ActorSystem("concurrent-board", ConfigFactory.load())
  
  val staticServer = system.actorOf(Props[StaticServer], name = "static-server")
  IO(Http) ! Http.Bind(staticServer, interface = "0.0.0.0", port = 9000)
  
  val server = system.actorOf(Props[Server], "server")
  server ! Start
}
