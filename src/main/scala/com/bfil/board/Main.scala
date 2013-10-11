package com.bfil.board

import scala.concurrent.duration.DurationDouble

import com.bfil.board.actors.{Board, Client, Note}
import com.bfil.board.messages.{AddNote, ConnectClient, Drop, Grab}
import com.typesafe.config.ConfigFactory

import akka.actor.{ActorSystem, Props, actorRef2Scala}

object Main extends App {

  val system = ActorSystem("concurrent-board", ConfigFactory.load())

  val board = system.actorOf(Props[Board], "board")
  val client = system.actorOf(Client.props("client"), "client")
  val client2 = system.actorOf(Client.props("client2"), "client2")
  val note = system.actorOf(Props[Note], "note")
  val note2 = system.actorOf(Props[Note], "note2")

  sequence(0.05,
    board ! AddNote,
    board ! ConnectClient("127.0.0.1"),
    client ! Grab(note),
    client2 ! Grab(note),
    client ! Drop,
    client2 ! Grab(note),
    client ! Grab(note2),
    client ! Grab(note),
    client ! Drop)

  def sequence(delay: Double, tasks: ByName[Unit]*) = {
    tasks.foldLeft(0.0)((time, task) => {
      system.scheduler.scheduleOnce(time seconds)(task())(system.dispatcher)
      time + delay
    })
  }
  
  implicit class ByName[T]( f: => T ) {
    def apply(): T = f
  }
}
