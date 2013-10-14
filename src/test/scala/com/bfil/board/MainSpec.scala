package com.bfil.board

import scala.concurrent.duration.DurationInt

import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.matchers.ShouldMatchers

import com.bfil.board.actors.{Note, User}
import com.bfil.board.messages.{Drop, Grab}

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
  
  "An user" should "be able to grab a note" in {
    val user = TestActorRef(User.props("user"))
    val note = TestActorRef(Props[Note])
    user ! Grab(note)
    user.underlyingActor.asInstanceOf[User].grabbedItem.get should be(note)
    note.underlyingActor.asInstanceOf[Note].owner.get should be(user)
  }
  
  "An user" should "not be able to grab a note grabbed by another user" in {
    val user = TestActorRef(User.props("user"))
    val user2 = TestActorRef(User.props("user2"))
    val note = TestActorRef(Props[Note])
    user ! Grab(note)
    user2 ! Grab(note)
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(Some(note))
    note.underlyingActor.asInstanceOf[Note].owner should be(Some(user))
    user2.underlyingActor.asInstanceOf[User].grabbedItem should be(None)
  }
  
  "An user" should "be able to drop a note after grabbing it" in {
    val user = TestActorRef(User.props("user"))
    val note = TestActorRef(Props[Note])
    user ! Grab(note)
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(Some(note))
    note.underlyingActor.asInstanceOf[Note].owner should be(Some(user))
    user ! Drop
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(None)
    note.underlyingActor.asInstanceOf[Note].owner should be(None)
  }
  
  "An user" should "drop the current grabbed note before grabbing a new one" in {
    val user = TestActorRef(User.props("user"))
    val note = TestActorRef(Props[Note])
    val note2 = TestActorRef(Props[Note])
    user ! Grab(note)
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(Some(note))
    note.underlyingActor.asInstanceOf[Note].owner should be(Some(user))
    user ! Grab(note2)
    user.underlyingActor.asInstanceOf[User].grabbedItem should be(Some(note2))
    note2.underlyingActor.asInstanceOf[Note].owner should be(Some(user))
    note.underlyingActor.asInstanceOf[Note].owner should be(None)
  }
  
}
