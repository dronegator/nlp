package com.github.dronegator.nlp.main.phrase


import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatchers, Route}
import com.github.dronegator.nlp.common.{Weight, Probability}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Word
import com.github.dronegator.nlp.vocabulary.VocabularyTools._
import com.github.dronegator.nlp.vocabulary.{VocabularyImpl, Vocabulary}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by cray on 9/12/16.
 */
object SuggestNextHandler extends DefaultJsonProtocol {

  case class Data()

  implicit val dataFormat = jsonFormat0(Data)

  implicit val requestFormat = jsonFormat2(Request[Data])
}


class SuggestNextHandler(vocabulary: VocabularyImpl)(implicit context: ExecutionContext)
  extends Handler[Request[SuggestNextHandler.Data], Response] {

  import SuggestNextHandler._
  import PathMatchers._

  def route: Route =
    pathSuffix("next") {
      pathPrefix(Segments(0, 100)) { words =>
        get {
          parameter('data.as[String]) {
            data =>
              complete {
                handle(Request(
                  phrase = words :+ ".",
                  data = data.parseJson.convertTo[Data]))
              }
          }
        }
      }
    }


  def handle(request: Request[Data]): Future[Response] = Future {
    val statement = vocabulary.tokenize(request.phrase.mkString(" "))

    val suggest = vocabulary
      .suggestForNext(statement)
      .flatMap {
        case (token, probability) =>
          vocabulary.wordMap.get(token).map { word =>
            SuggestedWord(word, probability)
          }
      }

    Response(suggest = suggest)
  }
}
