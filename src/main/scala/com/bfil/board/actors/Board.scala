package com.bfil.board.actors

import com.bfil.board.messages.{AddNote, ConnectClient}
import akka.actor.{Actor, ActorRef}
import akka.actor.Props
import akka.actor.ActorLogging

class Board extends Actor with ActorLogging {
  var clients: List[ActorRef] = Nil
  var notes: List[ActorRef] = Nil
  var clientId = 0
  var noteId = 0
  
  def receive = {
    case ConnectClient(ipAddress) => {
      clientId+=1
      val newClient = context.actorOf(Client.props(ipAddress), s"client$clientId")
      clients ::= newClient
      log.info(s"${newClient.path.name} connected")
    }
    case AddNote => {
      noteId+=1
      val newNote = context.actorOf(Props[Note], s"note$noteId")
      notes ::= newNote
      log.info(s"${newNote.path.name} added")
    }
    case _ => Unit
  }
}