package com.github.dronegator.nlp.main.phrase

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatchers, Route}
import com.github.dronegator.nlp.common.{Probability, Weight}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Word
import com.github.dronegator.nlp.main.phrase._
import com.github.dronegator.nlp.vocabulary.VocabularyTools._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyImpl}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import com.github.dronegator.nlp.main.Handler
import spray.json._
import com.github.dronegator.nlp.main.phrase.PhraseResponse._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by cray on 9/15/16.
 */

object GenerateHandler extends DefaultJsonProtocol {

  case class Data()

  implicit val dataFormat = jsonFormat0(Data)

  implicit val requestFormat = jsonFormat2(Request[Data])
}

class GenerateHandler(vocabulary: Vocabulary)(implicit context: ExecutionContext)
  extends Handler[Request[GenerateHandler.Data], Response[String]] {

  import GenerateHandler._
  import PathMatchers._

  def route: Route =
    pathSuffix("generate") {
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

  override def handle(request: Request[Data]): Future[Response[String]] = Future {
    val tokens = vocabulary.toTokens(request.phrase)

    val suggest = vocabulary
      .generatePhrase(tokens)
      .map{
        case statement =>
          Suggest(vocabulary.untokenize(statement), vocabulary.probability(statement))
      }


    Response(suggest = suggest.toList)
  }
}
