package com.github.dronegator.nlp.main.phrase


import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatchers, Route}
import com.github.dronegator.nlp.common.{Weight, Probability}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Word
import com.github.dronegator.nlp.main.phrase.SuggestForTheSameHandler._
import com.github.dronegator.nlp.vocabulary.VocabularyTools._
import com.github.dronegator.nlp.vocabulary.{VocabularyImpl, Vocabulary}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by cray on 9/12/16.
 */
object SuggestHandler extends DefaultJsonProtocol {

  case class Data()

  case class Response(continue: List[SuggestedWord],
                      next: List[SuggestedWord],
                      theSame: List[SuggestedWord])

  implicit val responseFormat = jsonFormat3(Response)


  implicit val dataFormat = jsonFormat0(Data)

  implicit val requestFormat = jsonFormat2(Request[Data])
}

class SuggestHandler(vocabulary: VocabularyImpl)(implicit context: ExecutionContext)
  extends Handler[Request[SuggestHandler.Data], SuggestHandler.Response] {

  import SuggestHandler._
  import PathMatchers._

  def route: Route =
  //pathSuffix("suggest") {
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
      //}
    }

  def handle(request: Request[Data]): Future[SuggestHandler.Response] = Future {
    val statement = vocabulary.tokenize(request.phrase.mkString(" ") + ".")

    val statementPrefix = vocabulary.tokenize(request.phrase.mkString(" "))

    val continue = vocabulary
      .continueStatement(statementPrefix)
      .flatMap {
        case (token, probability) =>
          vocabulary.wordMap.get(token)
            .map { word =>
              SuggestedWord(word, probability)
            }

      }
      .reverse

    val next = vocabulary
      .suggestForNext(statement)
      .flatMap {
        case (token, probability) =>
          vocabulary.wordMap.get(token).map { word =>
            SuggestedWord(word, probability)
          }
      }

    val theSame = vocabulary
      .suggestForTheSame(statement)
      .flatMap {
        case (token, probability) =>
          vocabulary.wordMap.get(token).map { word =>
            SuggestedWord(word, probability)
          }
      }

    Response(
      continue = continue,
      next = next,
      theSame = theSame
    )
  }
}
