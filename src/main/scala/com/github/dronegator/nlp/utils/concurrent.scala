package com.github.dronegator.nlp.utils

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, duration}, duration.Duration.Inf


object concurrent {
  implicit class Zukunft[A](future: Future[A]) {
    def await = 
      Await.result(future, Inf)
    
    def await(atMost: Duration) =
      Await.result(future, atMost)
  }
}