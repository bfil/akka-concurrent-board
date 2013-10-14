package com.bfil.board.actors

import com.bfil.board.messages.{ AddNote, CannotJoin, Joined, Join, Quit }

import akka.actor.{ Actor, ActorLogging, ActorRef, Kill, Props, actorRef2Scala }

class Board extends Actor with ActorLogging {
  var users = Map.empty[String, ActorRef]
  var notes = Set.empty[ActorRef]
  var noteId = 0

  def receive = {
    case Join(username) => {
      if (!users.contains(username)) {
        val user = context.actorOf(User.props(username), username)
        users = users + (username -> user)
        sender ! Joined(username)
        log.info(s"$username joined")
      } else {
        sender ! CannotJoin("Username already taken")
        log.info(s"$username cannot connect: Username already taken")
      }
    }
    case Quit(username) => {
      if (users.contains(username)) {
        log.info(s"$username disconnected")
        users.get(username).map(_ ! Kill)
        users = users - username
      }
    }
    case AddNote() => {
      noteId += 1
      val newNote = context.actorOf(Props[Note], s"note-$noteId")
      notes = notes + newNote
      log.info(s"${newNote.path.name} added")
    }
    case x => log.info(s"Unknown message: ${x.toString}")
  }
}