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

  case class SuggestedWord(word: Word, weight: Weight)

  case class Response(suggest: List[SuggestedWord])

  implicit val suggestedWordFormat = jsonFormat2(SuggestedWord)

  implicit val responseFormat = jsonFormat1(Response)

  trait Handler[I, O] {
    def route: Route

    def handle(request: I): Future[O]
  }
}
