package com.bfil.board.actors

import scala.concurrent.duration.DurationInt

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import spray.http.MediaType
import spray.http.MediaTypes.register
import spray.routing.Directive.pimpApply
import spray.routing.HttpService

class StaticServer extends Actor with ActorLogging with HttpService {
  implicit val timeout: Timeout = 1 second
  import context.dispatcher

  def actorRefFactory = context

  val LessType = register(
    MediaType.custom(
      mainType = "text",
      subType = "css",
      compressible = true,
      binary = false,
      fileExtensions = Seq("less")))

  val routes = {
    get {
      path("") {
        getFromFile("src/main/scala/com/bfil/board/public/index.html")
      } ~
      pathPrefix("public") {
        getFromDirectory("src/main/scala/com/bfil/board/public")
      }
    }
  }

  def receive = runRoute(routes)
}