package com.github.dronegator.nlp.main.phrase


import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatchers, Route}
import com.github.dronegator.nlp.common.{Probability, Weight}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Word
import com.github.dronegator.nlp.vocabulary.VocabularyTools._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyImpl}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.github.dronegator.nlp.main.Handler
import spray.json._
import com.github.dronegator.nlp.main.phrase.WordResponse._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by cray on 9/12/16.
 */
object ContinueHandler extends DefaultJsonProtocol {

  case class Data()

  implicit val dataFormat = jsonFormat0(Data)

  implicit val requestFormat = jsonFormat2(Request[Data])
}

class ContinueHandler(vocabulary: Vocabulary)(implicit context: ExecutionContext)
  extends Handler[Request[ContinueHandler.Data], Response[Word]] {

  import ContinueHandler._
  import PathMatchers._

  def route: Route =
    pathSuffix("continue") {
      pathPrefix(Segments(0, 100)) { words =>
        get {
          parameter('data.as[String]) {
            data =>
              complete {
                handle(Request(
                  phrase = words,
                  data = data.parseJson.convertTo[Data]))
              }
          }
        }
      }
    }

  override def handle(request: Request[Data]): Future[Response[Word]] = Future {
    val statement = vocabulary.tokenize(request.phrase.mkString(" "))

    println(statement)

    val suggest = vocabulary
      .continueStatement(statement)
      .flatMap {
        case (token, probability) =>
          vocabulary.wordMap.get(token)
            .map { word =>
              Suggest(word, probability)
            }

      }
      .reverse

    Response(suggest = suggest)
  }
}
