package com.bfil.board

import scala.concurrent.duration.DurationInt

import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.matchers.ShouldMatchers

import akka.actor.{ActorSystem, Props, actorRef2Scala}
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
  
  /*
  "A Greeter" should "be able to set a new greeting" in {
    val greeter = TestActorRef(Props[Greeter])
    greeter ! WhoToGreet("test")
    greeter.underlyingActor.asInstanceOf[Greeter].greeting should be("hello, test")
  }

  it should "be able to get a new greeting" in {
    val greeter = system.actorOf(Props[Greeter], "greeter")
    greeter ! WhoToGreet("test")
    greeter ! Greet
    expectMsgType[Greeting].message.toString should be("hello, test")
  }
  */
}
