package com.bfil.board.actors

import scala.util.Random
import com.bfil.board.messages.{AddNote, BoardUpdate, CannotJoin, Join, Joined, Quit}
import akka.actor.{Actor, ActorLogging, ActorRef, Kill, Props, actorRef2Scala}
import com.bfil.board.messages.MoveNote
import akka.pattern.ask
import com.bfil.board.messages.GetState
import akka.util.Timeout
import scala.concurrent.duration._
import com.bfil.board.messages.NoteState
import scala.concurrent.Future
import com.bfil.board.messages.Move
import com.bfil.board.messages.UpdateBoard

class Board extends Actor with ActorLogging {
  
  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(1 second)
  
  var users = Map.empty[String, ActorRef]
  var notes = Map.empty[Int, ActorRef]
  
  def boardUpdated() = {
    val futureStates = notes.map {
      case (noteId, noteRef) =>
        noteRef ? GetState map {
          case n @ NoteState(id, text, x, y) => n
        }
    }
    
    Future.sequence(futureStates).map {
      states => context.parent ! BoardUpdate(users.keys.toList, states.toList)
    }    
  }

  def receive = {
    
    case Join(username) =>
      if (!users.contains(username)) {
        val user = context.actorOf(User.props(username), username)
        users += (username -> user)
        sender ! Joined(username)
        boardUpdated()
        log.info(s"$username joined")
      } else {
        val error = "username already taken"
        sender ! CannotJoin(error)
        log.info(s"$username cannot connect: $error")
      }
    
    case Quit(username) =>
      if (users.contains(username)) {
        log.info(s"$username disconnected")
        users.get(username).map(_ ! Kill)
        users -= username
        boardUpdated()
      }
    
    case AddNote(text) =>
      val noteId = Random.nextInt
      val newNote = context.actorOf(Note.props(noteId, text), s"note-$noteId")
      notes += noteId -> newNote
      boardUpdated()
    
    case MoveNote(id, x, y) =>
      notes.get(id).foreach(_ ! Move(x,y))
      
    case UpdateBoard =>
      boardUpdated()
    
    case x => log.info(s"Unknown message: ${x.toString}")
  }
}