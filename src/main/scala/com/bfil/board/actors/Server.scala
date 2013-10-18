package com.bfil.board.actors

import org.json4s.DefaultFormats
import org.json4s.jackson.Serialization.write

import com.bfil.board.messages.Server.{Start, Stop}
import com.bfil.board.messages.WebSocket.{ClientConnected, ClientDisconnected, WebSocketMessage}

import akka.actor.{Actor, ActorRef, actorRef2Scala}
import io.backchat.hookup.{Connected, Disconnected, Error, HookupServer, HookupServerClient, JsonMessage, OutboundMessage, TextMessage}

class Server extends Actor {

  val server: HookupServer = (HookupServer(8125) {
    new HookupServerClient {
      
      def sendProxy(message: AnyRef) = send(message)
      def broadcastProxy(message: AnyRef) = broadcast(message)

      def receive: PartialFunction[io.backchat.hookup.InboundMessage,Unit] = {
        case Connected =>
          webSocketManager ! ClientConnected(id, sendProxy, broadcastProxy)

        case Disconnected(_) =>
          webSocketManager ! ClientDisconnected(id)

        case error @ Error(ex) =>
          System.err.println(s"Received an error: $error")
          ex foreach { _.printStackTrace(System.err) }

        case text: TextMessage =>
          webSocketManager ! (id, text.content)

        case json: JsonMessage =>
          webSocketManager ! WebSocketMessage(id, json.content)
      }
    }
  })
  
  def broadcastProxy(message: AnyRef) = server.broadcast(message)
  val webSocketManager: ActorRef = context.actorOf(WebSocket.props(broadcastProxy), "websocket")
  
  def receive = {
    case Start => server.start
    case Stop => server.stop
  }

  implicit def stringToTextMessage(s: String) = TextMessage(s)
  implicit def toJson(message: AnyRef): OutboundMessage = {
    implicit val formats = DefaultFormats
    "{\"message\":\"" + message.getClass.getSimpleName.replace("$", "") + "\",\"data\":" + write(message) + "}"
  }
}