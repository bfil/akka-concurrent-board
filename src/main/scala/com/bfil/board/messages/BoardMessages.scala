package com.bfil.board.messages

import akka.actor.ActorRef

case object AddNote
case class Join(username: String)
case class Quit(username: String)
case class Connected
case class CannotConnect(error: String)

case object Grab
case object Grabbed
case object NotGrabbed
case object Drop

case class Grab(item: ActorRef)