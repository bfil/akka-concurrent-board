package com.bfil.board.actors

import scala.annotation.migration
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

import com.bfil.board.messages.{Board => BoardMessages}
import com.bfil.board.messages.Note.{GetState, NoteState}
import com.bfil.board.messages.{User => UserMessages}
import com.bfil.board.messages.WebSocket.BoardUpdate

import akka.actor.{Actor, ActorLogging, ActorRef, Kill, actorRef2Scala}
import akka.pattern.ask
import akka.util.Timeout

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

    case BoardMessages.Join(username) =>
      if (users.forall(_._1.toLowerCase != username.toLowerCase)) {

        if (username.matches("[a-zA-Z0-9]{3,14}")) {
          val user = context.actorOf(User.props(username), username)
          users += (username -> user)
          sender ! BoardMessages.Joined(username)
          boardUpdated()
          log.info(s"$username joined")
        } else {
          val error = "username must only contain alphanumeric characters and must be between 3 and 14 characters long"
          sender ! BoardMessages.CannotJoin(error)
          log.info(s"$username cannot connect: $error")
        }
      } else {
        val error = "username already taken"
        sender ! BoardMessages.CannotJoin(error)
        log.info(s"$username cannot connect: $error")
      }

    case BoardMessages.Quit(username) =>
      if (users.contains(username)) {
        log.info(s"$username disconnected")

        users.get(username).map {
          user =>
            user ! UserMessages.DropNote
            user ! Kill
            users -= username
            boardUpdated()
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
      users.get(username).foreach(
        user =>
          notes.get(noteId).foreach(
            note => {
              user ! UserMessages.MoveNote(note, x, y)
              boardUpdated()
            }))
            
    case BoardMessages.RemoveNote(username, noteId) =>
      users.get(username).foreach(
        user =>
          notes.get(noteId).foreach(
            note => {
              user ! UserMessages.RemoveNote(note)
            }))

    case BoardMessages.GrabNote(username, noteId) =>
      users.get(username).foreach(
        user =>
          notes.get(noteId).foreach(
            note => {
              val requester = sender
              user ? UserMessages.GrabNote(note) map {
                case x =>
                  requester ! x
                  boardUpdated()
              }
            }))
            
    case BoardMessages.NoteRemoved(id) =>
      notes -= id
      boardUpdated()

    case BoardMessages.Update =>
      boardUpdated()

    case x => log.info(s"Unknown message: ${x.toString}")
  }
}