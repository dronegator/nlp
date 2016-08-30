package com.github.dronegator.nlp.main

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

/**
 * Created by cray on 8/30/16.
 */

trait Concurent {
  implicit val context = scala.concurrent.ExecutionContext.global

  implicit val system = ActorSystem()

  implicit val mat = ActorMaterializer()
}
