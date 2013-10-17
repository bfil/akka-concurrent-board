package com.bfil.board.actors

import com.bfil.board.messages.GetUsername
import com.bfil.board.messages.Note.{Drop, Grab, Grabbed, NotGrabbed}
import akka.actor.{Actor, ActorLogging, ActorRef, Props, actorRef2Scala}
import com.bfil.board.messages.User.GrabNote
import com.bfil.board.messages.User.MoveNote
import com.bfil.board.messages.Note.Move

class User(username: String) extends Actor with ActorLogging {
  var grabbedNote: Option[ActorRef] = None
  
  def dropNote = {
    if(grabbedNote.isDefined) log.info(s"dropping ${grabbedNote.map(_.path.name).get}")
    else log.info(s"nothing to drop")
    grabbedNote.foreach(_ ! Drop)
    grabbedNote = None
  }
  
  def receive = {
    
    case GetUsername =>
      sender ! username
    
    case GrabNote(note) =>
      log.info(s"grabbing ${note.path.name}")
      dropNote
      note ! Grab
      context.become(grabbing(sender, note))
      
    case MoveNote(note, x, y) =>
      note ! Move(x, y)
      
    case Drop =>
      dropNote
  }
  
  def grabbing(notify: ActorRef, note: ActorRef): Receive = {
    
    case Grabbed =>
      grabbedNote = Some(note)
      notify ! Grabbed
      log.info(s"grabbed ${note.path.name}")
      context.unbecome()
    
    case NotGrabbed =>
      notify ! NotGrabbed
      log.info(s"could not grab ${note.path.name}")
      context.unbecome()
    
  }
}

object User {
  def props(name: String): Props = Props(classOf[User], name)
}