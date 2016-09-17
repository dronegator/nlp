package com.github.dronegator.nlp.main.system

/**
  * Created by cray on 9/17/16.
  */

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.dronegator.nlp.main.Handler
import com.github.dronegator.nlp.main.Handler.RequestEmpty
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import spray.json._

import scala.concurrent.{ExecutionContext, Future}


object NTLPWebSystemVocabularyStatHandler extends DefaultJsonProtocol {

  //case class Request()

  case class ResponseVocabularyStat(meaningSize: Int,
                                    nGram1Size: Int,
                                    nGram2Size: Int,
                                    nGram3Size: Int,
                                    phraseCorrelationInnerSize: Int,
                                    phraseCorrelationOuterSize: Int)

  implicit val suggestPhraseFormat = jsonFormat6(ResponseVocabularyStat)
}

class NTLPWebSystemVocabularyStatHandler(vocabulary: VocabularyImpl)(implicit context: ExecutionContext)
  extends Handler[RequestEmpty, NTLPWebSystemVocabularyStatHandler.ResponseVocabularyStat] {


  import NTLPWebSystemVocabularyStatHandler._

  def route: Route =
    path("vocabulary") {
      get {
        complete {
          handle(RequestEmpty())
        }
      }
    }

  override def handle(request: RequestEmpty): Future[NTLPWebSystemVocabularyStatHandler.ResponseVocabularyStat] = Future {
    ResponseVocabularyStat(
      meaningSize = vocabulary.meaningMap.size,
      nGram1Size = vocabulary.nGram1.size,
      nGram2Size = vocabulary.nGram2.size,
      nGram3Size = vocabulary.nGram3.size,
      phraseCorrelationInnerSize = vocabulary.phraseCorrelationInner.size,
      phraseCorrelationOuterSize = vocabulary.phraseCorrelationConsequent.size)
  }
}

