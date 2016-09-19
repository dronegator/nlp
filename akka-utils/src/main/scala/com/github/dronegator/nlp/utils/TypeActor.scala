package com.github.dronegator.nlp.utils

import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.util.Timeout

import scala.reflect.ClassTag


/**
  * Created by cray on 9/18/16.
  */

case class TypeActorRef[A](actorRef: ActorRef)(implicit tag: ClassTag[A]) {
  def push(message: A) =
    actorRef ! message

  def ask(message: A)(implicit timeout: Timeout) =
    (new AskableActorRef(actorRef) ? message).mapTo[A]
}

