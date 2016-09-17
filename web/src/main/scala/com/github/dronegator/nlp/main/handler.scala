package com.github.dronegator.nlp.main

import akka.http.scaladsl.server._
import com.github.dronegator.nlp.common._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.main.phrase.ContinueHandler._

import scala.concurrent.Future

/**
  * Created by cray on 9/17/16.
  */

object Handler {
  case class RequestEmpty()

  case class ResponseEmpty()

  implicit val emptyRequestFormat = jsonFormat0(RequestEmpty)

  implicit val emptyResponseFormat = jsonFormat0(ResponseEmpty)
}
trait Handler[I, O] {
  def route: Route

  def handle(request: I): Future[O]
}
