package com.github.dronegator.nlp.main.session

import akka.actor.{Actor, Props, Terminated}
import com.github.dronegator.nlp.main.session.SessionManager.{CreateSession, SessionRef}
import com.github.dronegator.nlp.main.session.SessionStorage.SessionMessage
import com.github.dronegator.nlp.utils.typeactor._

/**
 * Created by cray on 9/18/16.
 */
case class SessionManagerConfig()
object SessionManager {

  def props(cfg: SessionManagerConfig, sessionStorage: TypeProps[SessionMessage]): TypeProps[SessionManagerMessage] =
    Props(new SessionManager(cfg, sessionStorage))

  trait SessionManagerMessage

  case class CreateSession(name: SessionId) extends SessionManagerMessage

  case class SessionRef(name: SessionId, typeActorRef: TypeActorRef[SessionMessage]) extends SessionManagerMessage
}

class SessionManager(cfg: SessionManagerConfig, sessionStorage: TypeProps[SessionMessage]) extends Actor {
  override def receive: Receive =
    receive(Map())

  def receive(map: Map[SessionId, TypeActorRef[SessionMessage]]): Receive = {
    case CreateSession(name) =>
      map.get(name) match {
        case Some(actorRef) =>
          sender() ! SessionRef(name, actorRef)

        case None =>
          val actorRef = context.actorOf(sessionStorage)

          context.become(receive(map + (name -> actorRef)))

          context.watch(actorRef)

          sender() ! SessionRef(name, actorRef)
      }

    case Terminated(actorRef) =>
      context.become(receive(map - SessionId(actorRef.path.name)))

    case x =>
      println(x)

  }
}
