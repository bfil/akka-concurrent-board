package com.bfil.board

import scala.concurrent.duration.DurationInt
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.matchers.ShouldMatchers
import com.bfil.board.actors.{Note, User}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.bfil.board.messages.User.GrabNote
import com.bfil.board.messages.User.DropNote

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
  
  "An user" should "be able to grab a note" in {
    val user = TestActorRef(User.props("user"))
    val note = TestActorRef(Note.props(1, "Test"))
    user ! GrabNote(note)
    user.underlyingActor.asInstanceOf[User].grabbedNote.get should be(note)
    note.underlyingActor.asInstanceOf[Note].owner.get should be(user)
  }
  
  "An user" should "not be able to grab a note grabbed by another user" in {
    val user = TestActorRef(User.props("user"))
    val user2 = TestActorRef(User.props("user2"))
    val note = TestActorRef(Note.props(1, "Test"))
    user ! GrabNote(note)
    user2 ! GrabNote(note)
    user.underlyingActor.asInstanceOf[User].grabbedNote should be(Some(note))
    note.underlyingActor.asInstanceOf[Note].owner should be(Some(user))
    user2.underlyingActor.asInstanceOf[User].grabbedNote should be(None)
  }
  
  "An user" should "be able to drop a note after grabbing it" in {
    val user = TestActorRef(User.props("user"))
    val note = TestActorRef(Note.props(1, "Test"))
    user ! GrabNote(note)
    user.underlyingActor.asInstanceOf[User].grabbedNote should be(Some(note))
    note.underlyingActor.asInstanceOf[Note].owner should be(Some(user))
    user ! DropNote
    user.underlyingActor.asInstanceOf[User].grabbedNote should be(None)
    note.underlyingActor.asInstanceOf[Note].owner should be(None)
  }
  
  "An user" should "drop the current grabbed note before grabbing a new one" in {
    val user = TestActorRef(User.props("user"))
    val note = TestActorRef(Note.props(1, "Test"))
    val note2 = TestActorRef(Note.props(2, "Test"))
    user ! GrabNote(note)
    user.underlyingActor.asInstanceOf[User].grabbedNote should be(Some(note))
    note.underlyingActor.asInstanceOf[Note].owner should be(Some(user))
    user ! GrabNote(note2)
    user.underlyingActor.asInstanceOf[User].grabbedNote should be(Some(note2))
    note2.underlyingActor.asInstanceOf[Note].owner should be(Some(user))
    note.underlyingActor.asInstanceOf[Note].owner should be(None)
  }
  
}
