package com.github.dronegator.nlp.main.session

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorRef, ActorRefFactory, ActorSystem, Kill, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.github.dronegator.nlp.main.session.Session._
import com.github.dronegator.nlp.utils.{CFG, TypeActorRef}

import scala.reflect.ClassTag

/**
  * Created by cray on 9/18/16.
  */

object Session {
  sealed trait SessionMessage

  case class Set[A](name: String, value: A) extends SessionMessage

  case class Get[A](name: String) extends SessionMessage

  case class Value[A](name: String, value: Option[A]) extends SessionMessage

  case object Ping extends SessionMessage

  case object Pong extends SessionMessage

  case object Tick extends SessionMessage

  case class ClosableValue[A](value: A, f: A => Unit) {
    def close() =
      f(value)
  }

  def wrap(cfg: CFG)(implicit system: ActorRefFactory) =
    TypeActorRef[SessionMessage](system.actorOf(props(cfg)))

  def wrap(cfg: CFG, name: String)(implicit system: ActorRefFactory) =
    TypeActorRef[SessionMessage](system.actorOf(props(cfg), name))

  def props(cfg: CFG): Props =
    Props(new Session(cfg))
}




class Session(cfg: CFG) extends Actor {
  override def receive: Receive = receive(Map(), System.currentTimeMillis())

  def receive(map: Map[String, Any], time: Long): Receive = {
      case Set(name, value) =>
        context.become(receive(map + (name -> value), System.currentTimeMillis()))

      case Get(name) =>
        sender() ! Value(name, map.get(name))
        context.become(receive(map, System.currentTimeMillis()))

      case Tick =>
        if (System.currentTimeMillis() - time > 600) {
          map.values
              .foreach {
                case value: ClosableValue[_] =>
                    value.close()
                case _ =>
              }

          self ! Kill
        }

      case Ping =>




  }

}