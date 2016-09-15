package com.github.dronegator.nlp.main

import akka.http.scaladsl.server._
import com.github.dronegator.nlp.common._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.main.phrase.ContinueHandler._
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import com.softwaremill.macwire._

import scala.concurrent.Future

/**
 * Created by cray on 9/12/16.
 */
package object phrase {
  case class Request[D](phrase: List[Word], data: D)

  case class Suggest[A](value: A, weight: Weight)

  case class Response[A](suggest: List[Suggest[A]])

  object PhraseResponse {
    implicit val suggestPhraseFormat = jsonFormat2(Suggest[String])

    implicit val responsePhraseFormat = jsonFormat1(Response[String])
  }

  object WordResponse {
    implicit val suggestWordFormat = jsonFormat2(Suggest[Word])

    implicit val responseWordFormat = jsonFormat1(Response[Word])

  }
 


  trait Handler[I, O] {
    def route: Route

    def handle(request: I): Future[O]
  }
}
