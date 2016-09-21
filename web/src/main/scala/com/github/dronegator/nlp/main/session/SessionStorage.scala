package com.github.dronegator.nlp.main.session

import akka.actor.{Actor, ActorLogging, ActorRefFactory, Kill, Props}
import com.github.dronegator.nlp.main.session.SessionStorage
import com.github.dronegator.nlp.main.session.SessionStorage._
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.utils.typeactor._

/**
  * Created by cray on 9/18/16.
  */

object SessionStorage {

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

  def props(cfg: CFG): TypeProps[SessionMessage] =
    Props(new SessionStorage(cfg))
}

class SessionStorage(cfg: CFG)
  extends Actor
  with ActorLogging {
  override def receive: Receive = receive(Map(), System.currentTimeMillis())

  def receive(map: Map[String, Any], time: Long): Receive = {
    case Set(name, value) =>
      log.info(s"Set name=$name to value=$value in ${self.path.elements.last}")
      println(s"Set name=$name to value=$value in ${self.path.elements.last}")
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
