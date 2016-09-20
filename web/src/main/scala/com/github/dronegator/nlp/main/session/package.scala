package com.github.dronegator.nlp.main

import akka.actor.ActorSystem
import akka.util.Timeout
import com.github.dronegator.nlp.main.session.SessionStorage.SessionMessage
import com.github.dronegator.nlp.utils.TypeActorRef
import com.softwaremill.tagging._

import scala.concurrent.{ExecutionContext, Future, duration}, duration._

/**
  * Created by cray on 9/20/16.
  */
package object session {
  trait SIdTag

  type SessionId = String @@ SIdTag

  object SessionId {
    def apply(sessionId: String) =
      sessionId.taggedWith[SIdTag]
  }

  trait SessionExt {
    protected def typeActorRef: Future[TypeActorRef[SessionMessage]]
    implicit def system: ActorSystem
    implicit def context: ExecutionContext
    implicit def timeout: Timeout = 10 seconds

    def /[A](name: String) =
      for {
        typeActorRef <- typeActorRef
        SessionStorage.Value(_, value) <- typeActorRef.ask(SessionStorage.Get(name))
      } yield {
        value.asInstanceOf[A]
      }

    def push[A](name: String, value: A) =
      for {
        typeActorRef <- typeActorRef
      } {
        typeActorRef.push(SessionStorage.Set(name, value))
      }
  }

  implicit class SessionIdExt(val sessionId: SessionId)(implicit val system: ActorSystem, val context: ExecutionContext) extends SessionExt {
    override protected def typeActorRef: Future[TypeActorRef[SessionMessage]] =
      system.actorSelection(s"akka://default/user/$sessionId")
        .resolveOne()
        .map {
          TypeActorRef[SessionMessage](_)
        }
  }

  implicit class TypeActorRefSessionExt(val tAR: TypeActorRef[SessionMessage])(implicit val system: ActorSystem, val context: ExecutionContext) extends SessionExt {
    override protected def typeActorRef: Future[TypeActorRef[SessionMessage]] =
      Future.successful(tAR)
  }

}
