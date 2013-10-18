package com.bfil.board.actors

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random
import com.bfil.board.messages.{ AddNote, BoardUpdate, CannotJoin, GetState, Grab, Join, Joined, Move, NoteState, Quit, UpdateBoard }
import com.bfil.board.messages.User.{ GrabNote, MoveNote }
import com.bfil.board.messages.Note.Drop
import akka.actor.{ Actor, ActorLogging, ActorRef, Kill, actorRef2Scala }
import akka.pattern.ask
import akka.util.Timeout
import com.bfil.board.messages.Remove
import com.bfil.board.messages.User.RemoveNote
import com.bfil.board.messages.NoteRemoved

class Board extends Actor with ActorLogging {

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(1 second)

  var users = Map.empty[String, ActorRef]
  var notes = Map.empty[Int, ActorRef]

  def boardUpdated() = {
    val futureStates = notes.map {
      case (noteId, noteRef) =>
        noteRef ? GetState map {
          case n @ NoteState(id, text, x, y, owner) => n
        }
    }

    Future.sequence(futureStates).map {
      states => context.parent ! BoardUpdate(users.keys.toList, states.toList)
    }
  }

  def receive = {

    case Join(username) =>
      if (!users.contains(username)) {

        if (username.matches("[a-zA-Z0-9]{3,14}")) {
          val user = context.actorOf(User.props(username), username)
          users += (username -> user)
          sender ! Joined(username)
          boardUpdated()
          log.info(s"$username joined")
        } else {
          val error = "username must only contain alphanumeric characters and must be between 3 and 14 characters long"
          sender ! CannotJoin(error)
          log.info(s"$username cannot connect: $error")
        }
      } else {
        val error = "username already taken"
        sender ! CannotJoin(error)
        log.info(s"$username cannot connect: $error")
      }

    case Quit(username) =>
      if (users.contains(username)) {
        log.info(s"$username disconnected")

        users.get(username).map {
          user =>
            user ! Drop
            user ! Kill
            users -= username
            boardUpdated()
        }
      }

    case AddNote(username, text) =>
      users.get(username) foreach { user =>
        val noteId = Random.nextInt
        val newNote = context.actorOf(Note.props(noteId, text), s"note-$noteId")
        notes += noteId -> newNote
        val requester = sender
        user ? GrabNote(newNote) map {
          case x =>
            requester ! x
            boardUpdated()
        }
      }

    case Move(username, noteId, x, y) =>
      users.get(username).foreach(
        user =>
          notes.get(noteId).foreach(
            note => {
              user ! MoveNote(note, x, y)
              boardUpdated()
            }))
            
    case Remove(username, noteId) =>
      users.get(username).foreach(
        user =>
          notes.get(noteId).foreach(
            note => {
              user ! RemoveNote(note)
            }))

    case Grab(username, noteId) =>
      users.get(username).foreach(
        user =>
          notes.get(noteId).foreach(
            note => {
              val requester = sender
              user ? GrabNote(note) map {
                case x =>
                  requester ! x
                  boardUpdated()
              }
            }))
            
    case NoteRemoved(id) =>
      notes -= id
      boardUpdated()

    case UpdateBoard =>
      boardUpdated()

    case x => log.info(s"Unknown message: ${x.toString}")
  }
}