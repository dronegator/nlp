package com.github.dronegator.nlp.utils.typeactor

/**
 * Created by cray on 9/20/16.
 */

import akka.actor._
import akka.pattern.AskableActorRef
import akka.util.Timeout

import scala.reflect.ClassTag

object TypeActorRef {
  implicit def actorRefToTypeActorRef[A](actorRef: ActorRef)(implicit tag: ClassTag[A]) =
    TypeActorRef[A](actorRef)
}

case class TypeActorRef[A](actorRef: ActorRef)(implicit tag: ClassTag[A]) {
  def push(message: A) =
    actorRef ! message

  def ask(message: A)(implicit timeout: Timeout) =
    (new AskableActorRef(actorRef) ? message).mapTo[A]
}
