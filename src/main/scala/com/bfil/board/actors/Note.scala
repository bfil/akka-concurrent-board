package com.bfil.board.actors

import scala.concurrent.duration.DurationInt
import scala.util.Random

import com.bfil.board.messages.Board.{ NoteRemoved, Update }
import com.bfil.board.messages.Note.{ Drop, GetState, Grab, Grabbed, Move, Edit, NotGrabbed, NoteState, Remove }
import com.bfil.board.messages.User.GetUsername

import akka.actor.{ Actor, ActorRef, Props, actorRef2Scala }
import akka.pattern.ask
import akka.util.Timeout

class Note(id: Int, _text: String) extends Actor {

  implicit val executionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(1 second)

  var text = _text
  var x = Random.nextInt(600)
  var y = Random.nextInt(300)
  var owner: Option[ActorRef] = None

  def ifOwner(actor: ActorRef)(f: => Unit) =
    owner match {
      case Some(ownedBy) if ownedBy == actor => f
      case _ =>
    }

  def receive = {

    case Grab =>
      owner match {
        case None =>
          owner = Some(sender); sender ! Grabbed
        case Some(ownedBy) if ownedBy == sender => sender ! Grabbed
        case _ => sender ! NotGrabbed
      }

    case Drop =>
      owner.map(o => if (sender.equals(o)) owner = None)

    case Move(_x, _y) =>
      ifOwner(sender) {
        x = _x
        y = _y
        context.parent ! Update
      }

    case Edit(_text) =>
      ifOwner(sender) {
        text = _text
        context.parent ! Update
      }

    case Remove =>
      ifOwner(sender) {
        context.parent ! NoteRemoved(id)
      }

    case GetState =>
      owner match {
        case Some(o) =>
          val requester = sender
          o ? GetUsername map {
            case username: String => requester ! NoteState(id, text, x, y, Some(username))
            case _ => sender ! NoteState(id, text, x, y, None)
          } recover {
            case e: Throwable => sender ! NoteState(id, text, x, y, None)
          }
        case None =>
          sender ! NoteState(id, text, x, y, None)
      }
  }
}

object Note {
  def props(id: Int, text: String): Props = Props(classOf[Note], id, text)
}