package com.bfil.board.actors

import scala.collection.immutable.ListMap
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random
import com.bfil.board.messages.{ Board => BoardMessages }
import com.bfil.board.messages.Note.{ GetState, NoteState }
import com.bfil.board.messages.{ User => UserMessages }
import com.bfil.board.messages.WebSocket.BoardUpdate
import akka.actor.{ Actor, ActorLogging, ActorRef, PoisonPill, actorRef2Scala }
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.Terminated

class Board extends Actor with ActorLogging {

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(1 second)

  var users = Map.empty[String, ActorRef]
  var notes = ListMap.empty[Int, ActorRef]

  def withUserAndNote(username: String, noteId: Int)(f: (ActorRef, ActorRef) => Unit) {
    users.get(username).foreach(
      user => {
        notes.get(noteId).foreach(
          note => f(user, note))
      })
  }

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

    case BoardMessages.Join(username) =>
      if (users.forall(_._1.toLowerCase != username.toLowerCase)) {

        if (username.matches("[a-zA-Z0-9]{3,14}")) {
          val user = context.actorOf(User.props(username), username)
          context.watch(user)
          users += (username -> user)
          sender ! BoardMessages.Joined(username)
          boardUpdated()
          log.info(s"$username joined")
        } else {
          val error = "username must only contain alphanumeric characters and must be between 3 and 14 characters long"
          sender ! BoardMessages.CannotJoin(error)
          log.info(s"$username cannot join: $error")
        }
      } else {
        val error = "username already taken"
        sender ! BoardMessages.CannotJoin(error)
        log.info(s"$username cannot join: $error")
      }

    case BoardMessages.Quit(username) =>
      if (users.contains(username)) {
        log.info(s"$username left")

        users.get(username).map {
          user =>
            user ! UserMessages.DropNote
            user ! PoisonPill
        }
      }

    case BoardMessages.AddNote(username, text) =>
      users.get(username) foreach { user =>
        val noteId = Random.nextInt
        val newNote = context.actorOf(Note.props(noteId, text), s"note-$noteId")
        notes += noteId -> newNote
        val requester = sender
        user ? UserMessages.GrabNote(newNote) map {
          case x =>
            requester ! x
            boardUpdated()
        }
      }

    case BoardMessages.MoveNote(username, noteId, x, y) =>
      withUserAndNote(username, noteId) {
        (user, note) =>
          notes = notes - noteId + (noteId -> note)
          user ! UserMessages.MoveNote(note, x, y)
          boardUpdated()
      }

    case BoardMessages.EditNote(username, noteId, text) =>
      withUserAndNote(username, noteId) {
        (user, note) =>
          notes = notes - noteId + (noteId -> note)
          user ! UserMessages.EditNote(note, text)
          boardUpdated()
      }

    case BoardMessages.RemoveNote(username, noteId) =>
      withUserAndNote(username, noteId) {
        (user, note) =>
          user ! UserMessages.RemoveNote(note)
      }

    case BoardMessages.GrabNote(username, noteId) =>
      withUserAndNote(username, noteId) {
        (user, note) =>
          val requester = sender
          user ? UserMessages.GrabNote(note) map {
            case x =>
              requester ! x
              boardUpdated()
          }
      }

    case BoardMessages.NoteRemoved(id) =>
      notes -= id
      boardUpdated()

    case BoardMessages.Update =>
      boardUpdated()

    case Terminated(actor) =>
      users.find(_._2 == actor).map(users -= _._1)
      boardUpdated()

    case x => log.info(s"Unknown message: ${x.toString}")
  }
}