package com.bfil.board

import scala.concurrent.duration.DurationInt
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.matchers.ShouldMatchers
import com.bfil.board.actors.{Note, Client}
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
  
  "A client" should "be able to grab a note" in {
    val client = TestActorRef(Client.props("client"))
    val note = TestActorRef(Props[Note])
    client ! Grab(note)
    client.underlyingActor.asInstanceOf[Client].grabbedItem.get should be(note)
    note.underlyingActor.asInstanceOf[Note].owner.get should be(client)
  }
  
  "A client" should "not be able to grab a note grabbed by another client" in {
    val client = TestActorRef(Client.props("client"))
    val client2 = TestActorRef(Client.props("client2"))
    val note = TestActorRef(Props[Note])
    client ! Grab(note)
    client2 ! Grab(note)
    client.underlyingActor.asInstanceOf[Client].grabbedItem should be(Some(note))
    note.underlyingActor.asInstanceOf[Note].owner should be(Some(client))
    client2.underlyingActor.asInstanceOf[Client].grabbedItem should be(None)
  }
  
  "A client" should "be able to drop a note after grabbing it" in {
    val client = TestActorRef(Client.props("client"))
    val note = TestActorRef(Props[Note])
    client ! Grab(note)
    client.underlyingActor.asInstanceOf[Client].grabbedItem should be(Some(note))
    note.underlyingActor.asInstanceOf[Note].owner should be(Some(client))
    client ! Drop
    client.underlyingActor.asInstanceOf[Client].grabbedItem should be(None)
    note.underlyingActor.asInstanceOf[Note].owner should be(None)
  }
  
  "A client" should "drop the current grabbed note before grabbing a new one" in {
    val client = TestActorRef(Client.props("client"))
    val note = TestActorRef(Props[Note])
    val note2 = TestActorRef(Props[Note])
    client ! Grab(note)
    client.underlyingActor.asInstanceOf[Client].grabbedItem should be(Some(note))
    note.underlyingActor.asInstanceOf[Note].owner should be(Some(client))
    client ! Grab(note2)
    client.underlyingActor.asInstanceOf[Client].grabbedItem should be(Some(note2))
    note2.underlyingActor.asInstanceOf[Note].owner should be(Some(client))
    note.underlyingActor.asInstanceOf[Note].owner should be(None)
  }
  
}
