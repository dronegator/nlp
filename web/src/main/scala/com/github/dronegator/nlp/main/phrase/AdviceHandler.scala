package com.github.dronegator.nlp.main.phrase

/**
 * Created by cray on 9/15/16.
 */

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{PathMatchers, Route}
import com.github.dronegator.nlp.common.{Weight, Probability}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, Word}
import com.github.dronegator.nlp.main.phrase._
import com.github.dronegator.nlp.vocabulary.VocabularyTools._
import com.github.dronegator.nlp.vocabulary.{VocabularyImpl, Vocabulary}
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._
import com.github.dronegator.nlp.main.phrase.PhraseResponse._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by cray on 9/15/16.
 */

object AdviceHandler extends DefaultJsonProtocol {

  case class Data(stickKeywords: Option[Boolean],
                  varyAuxiliary: Option[Boolean],
                  changeLimit: Option[Int],
                  uncertainty: Option[Double],
                  variability: Option[Int])

  implicit val dataFormat = jsonFormat5(Data)

  implicit val requestFormat = jsonFormat2(Request[Data])
}

class AdviceHandler(vocabulary: VocabularyImpl)(implicit context: ExecutionContext)
  extends Handler[Request[AdviceHandler.Data], Response[String]] {

  import AdviceHandler._
  import PathMatchers._

  def route: Route =
    pathSuffix("advice") {
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
    val statement = vocabulary.tokenize(request.phrase :+ ".")

    val suggest = vocabulary
      .adviceOptimal(
        statement,
        keywords = if (request.data.stickKeywords.getOrElse(false)) vocabulary.keywords(statement).map(_._1).toSet else Set[Token](),
        auxiliary = if (request.data.varyAuxiliary.getOrElse(true)) vocabulary.auxiliary else Set[Token](),
        changeLimit = request.data.changeLimit.getOrElse(3),
        uncertainty = request.data.uncertainty.getOrElse(0.0),
        variability = request.data.variability.getOrElse(7)
      )
      .map{
        case (statement, probability) =>
          Suggest(vocabulary.untokenize(statement), probability)
      }


    Response(suggest = suggest)
  }
}
