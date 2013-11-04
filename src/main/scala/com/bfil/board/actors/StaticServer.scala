package com.bfil.board.actors

import akka.actor.{Actor, ActorLogging}
import spray.http.MediaType
import spray.http.MediaTypes.register
import spray.routing.Directive.pimpApply
import spray.routing.HttpService

class StaticServer extends Actor with ActorLogging with HttpService {
  
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