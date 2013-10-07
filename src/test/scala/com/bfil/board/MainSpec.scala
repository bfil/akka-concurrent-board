package com.bfil.board

import scala.concurrent.duration.DurationInt

import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.matchers.ShouldMatchers

import com.bfil.board.actors.{StickyNote, User}
import com.bfil.board.messages.Grab

import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}

class MainSpec(_system: ActorSystem)
  extends TestKit(_system)
  with ImplicitSender
  with ShouldMatchers
  with FlatSpec
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("board-test"))

  override def afterAll: Unit = {
    system.shutdown()
    system.awaitTermination(10.seconds)
  }
  
  "An user" should "be able to grab a stickyNote" in {
    val user = TestActorRef(Props[User])
    val stickyNote = TestActorRef(Props[StickyNote])
    user ! Grab(stickyNote)
    user.underlyingActor.asInstanceOf[User].grabbedItem.get should be(stickyNote)
    stickyNote.underlyingActor.asInstanceOf[StickyNote].grabbedBy.get should be(user)
  }
  
  "An user" should "not be able to grab a stickyNote grabbed by another user" in {
    val user = TestActorRef(Props[User])
    val user2 = TestActorRef(Props[User])
    val stickyNote = TestActorRef(Props[StickyNote])
    user ! Grab(stickyNote)
    user2 ! Grab(stickyNote)
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(Some(stickyNote))
    stickyNote.underlyingActor.asInstanceOf[StickyNote].grabbedBy should be(Some(user))
    user2.underlyingActor.asInstanceOf[User].grabbedItem should be(None)
  }
  
}
