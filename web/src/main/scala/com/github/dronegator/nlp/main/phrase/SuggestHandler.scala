package com.github.dronegator.nlp.main.phrase


import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatchers, Route}
import com.github.dronegator.nlp.common.{Probability, Weight}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Word
import com.github.dronegator.nlp.main.phrase.SuggestForTheSameHandler._
import com.github.dronegator.nlp.vocabulary.VocabularyTools._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyImpl}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import PhraseResponse._
import com.github.dronegator.nlp.main.Handler

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by cray on 9/12/16.
 */
object SuggestHandler extends DefaultJsonProtocol {

  case class Data()

  case class Response(continue: List[Suggest[Word]],
                      next: List[Suggest[Word]],
                      theSame: List[Suggest[Word]],
                      probability: Double,
                      equalizedProbability: Double,
                      keywords: List[Word])

  implicit val responseFormat = jsonFormat6(Response)

  implicit val dataFormat = jsonFormat0(Data)

  implicit val requestFormat = jsonFormat2(Request[Data])
}

class SuggestHandler(vocabulary: VocabularyImpl)(implicit context: ExecutionContext)
  extends Handler[Request[SuggestHandler.Data], SuggestHandler.Response] {

  import SuggestHandler._
  import PathMatchers._

  def route: Route =
    pathPrefix(Segments(0, 100)) { words =>
      get {
        parameter('data.as[String]) {
          data =>
            complete {
              handle(Request[Data](
                  phrase = words,
                  data = data.parseJson.convertTo[Data]))
            }
        }
      }
    }

  def handle(request: Request[Data]): Future[SuggestHandler.Response] = Future {
    val statement = vocabulary.tokenize(request.phrase.mkString(" ") + ".")

    val statementPrefix = vocabulary.tokenize(request.phrase.mkString(" ")).dropRight(1)

    val continue = vocabulary
      .continueStatement(statementPrefix)
      .flatMap {
        case (token, probability) =>
          vocabulary.wordMap.get(token)
            .map { word =>
              Suggest(word, probability)
            }

      }
      .reverse

    val next = vocabulary
      .suggestForNext(statement)
      .flatMap {
        case (token, probability) =>
          vocabulary.wordMap.get(token).map { word =>
            Suggest(word, probability)
          }
      }

    val theSame = vocabulary
      .suggestForTheSame(statement)
      .flatMap {
        case (token, probability) =>
          vocabulary.wordMap.get(token).map { word =>
            Suggest(word, probability)
          }
      }

    val probability = vocabulary.probability(statementPrefix);

    val equalizedProbability = probability / vocabulary.statementDenominator(statementPrefix)

    val keywords = vocabulary.keywords(statementPrefix)
        .collect{
          case (word, (p, _, _)) if p > 0 =>
            word
        }
        .toList

    SuggestHandler.Response(
      continue = continue,
      next = next,
      theSame = theSame,
      probability = probability,
      equalizedProbability = equalizedProbability,
      keywords = vocabulary.toWords(keywords)
    )
  }
}
