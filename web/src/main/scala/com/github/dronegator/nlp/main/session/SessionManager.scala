package com.github.dronegator.nlp.main.session

import akka.actor.{Actor, ActorRefFactory, Props, Terminated}
import com.github.dronegator.nlp.main.session.SessionStorage.SessionMessage
import com.github.dronegator.nlp.main.session.SessionManager.{CreateSession, SessionRef}
import com.github.dronegator.nlp.utils.{CFG, TypeActorRef}

/**
  * Created by cray on 9/18/16.
  */

object SessionManager {

  trait SessionManagerMessage

  case class CreateSession(name: SessionId) extends SessionManagerMessage

  case class SessionRef(name: SessionId, typeActorRef: TypeActorRef[SessionMessage]) extends SessionManagerMessage

  def wrap(cfg: CFG)(implicit system: ActorRefFactory) =
    TypeActorRef[SessionManagerMessage](system.actorOf(props(cfg)))

  def wrap(cfg: CFG, name: String)(implicit system: ActorRefFactory) =
    TypeActorRef[SessionManagerMessage](system.actorOf(props(cfg), name))

  def props(cfg: CFG): Props =
    Props(new SessionManager(cfg))
}

class SessionManager(cfg: CFG) extends Actor {
  override def receive: Receive =
    receive(Map())

  def receive(map: Map[SessionId, TypeActorRef[SessionMessage]]): Receive = {
    case CreateSession(name) =>
      map.get(name) match {
        case Some(actorRef) =>
          sender() ! SessionRef(name, actorRef)

        case None =>
          val actorRef = SessionStorage.wrap(cfg, name)

          context.become(receive(map + (name -> actorRef)))
          context.watch(actorRef.actorRef)
          sender() ! SessionRef(name, actorRef)
      }

    case Terminated(actorRef) =>
      context.become(receive(map - SessionId(actorRef.path.name)))

    case x =>
      println(x)

  }
}
