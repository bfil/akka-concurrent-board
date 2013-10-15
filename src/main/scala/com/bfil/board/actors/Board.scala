package com.bfil.board.actors

import scala.util.Random

import com.bfil.board.messages.{AddNote, BoardUpdate, CannotJoin, Join, Joined, Quit}

import akka.actor.{Actor, ActorLogging, ActorRef, Kill, Props, actorRef2Scala}

class Board extends Actor with ActorLogging {
  var users = Map.empty[String, ActorRef]
  var notes = Map.empty[Int, ActorRef]
  var notesPositions = Map.empty[Int, (Int, Int)]
  var notesTexts = Map.empty[Int, String]
  var noteId = 0

  def receive = {
    case Join(username) => {
      if (!users.contains(username)) {
        val user = context.actorOf(User.props(username), username)
        users += (username -> user)
        sender ! Joined(username)
        context.parent ! BoardUpdate(notes.keys.toList, notesPositions.values.map{case(x,y) => List(x,y)}.toList, notesTexts.values.toList)
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
        users -= username
      }
    }
    case AddNote() => {
      noteId += 1
      val newNote = context.actorOf(Props[Note], s"note-$noteId")
      notes += (noteId -> newNote)
      notesPositions += (noteId -> (Random.nextInt(600),Random.nextInt(600)))
      notesTexts += (noteId -> Random.alphanumeric.take(10).mkString)
      log.info(s"${newNote.path.name} added")
      context.parent ! BoardUpdate(notes.keys.toList, notesPositions.values.map{case(x,y) => List(x,y)}.toList, notesTexts.values.toList)
    }
    case x => log.info(s"Unknown message: ${x.toString}")
  }
}