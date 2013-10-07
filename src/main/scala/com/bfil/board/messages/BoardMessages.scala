package com.bfil.board.messages

import akka.actor.ActorRef

case object Grab
case object Grabbed
case object NotGrabbed
case object Drop

case class Grab(item: ActorRef)