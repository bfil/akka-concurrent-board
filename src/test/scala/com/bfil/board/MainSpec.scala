package com.bfil.board

import scala.concurrent.duration.DurationInt
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.matchers.ShouldMatchers
import com.bfil.board.actors.{StickyNote, User}
import com.bfil.board.messages.Grab
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.bfil.board.messages.Drop

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
  
  "An user" should "be able to drop a stickyNote after grabbing it" in {
    val user = TestActorRef(Props[User])
    val stickyNote = TestActorRef(Props[StickyNote])
    user ! Grab(stickyNote)
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(Some(stickyNote))
    stickyNote.underlyingActor.asInstanceOf[StickyNote].grabbedBy should be(Some(user))
    user ! Drop
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(None)
    stickyNote.underlyingActor.asInstanceOf[StickyNote].grabbedBy should be(None)
  }
  
  "An user" should "drop the current grabbed note before grabbing a new one" in {
    val user = TestActorRef(Props[User])
    val stickyNote = TestActorRef(Props[StickyNote])
    val stickyNote2 = TestActorRef(Props[StickyNote])
    user ! Grab(stickyNote)
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(Some(stickyNote))
    stickyNote.underlyingActor.asInstanceOf[StickyNote].grabbedBy should be(Some(user))
    user ! Grab(stickyNote2)
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(Some(stickyNote2))
    stickyNote2.underlyingActor.asInstanceOf[StickyNote].grabbedBy should be(Some(user))
    stickyNote.underlyingActor.asInstanceOf[StickyNote].grabbedBy should be(None)
  }
  
}
