package com.github.dronegator.nlp.main.phrase

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import com.github.dronegator.nlp.main.NLPTWebServiceMain._
import com.github.dronegator.nlp.main.{Concurent, NLPTApp}
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import com.softwaremill.macwire._

/**
 * Created by cray on 9/12/16.
 */
object NLPTWebServicePhraseTrait {

}

trait NLPTWebServicePhraseTrait {
  this: NLPTApp with Concurent  =>

  lazy val continue: ContinueHandler = wire[ContinueHandler]

  lazy val suggestForNext: SuggestNextHandler = wire[SuggestNextHandler]

  lazy val suggestForTheSame: SuggestForTheSameHandler = wire[SuggestForTheSameHandler]

  lazy val suggest: SuggestHandler = wire[SuggestHandler]

  lazy val generate: GenerateHandler = wire[GenerateHandler]

  lazy val advice: AdviceHandler = wire[AdviceHandler]

  lazy val routePhrase = pathPrefix("phrase") { request =>
    val tStart = System.currentTimeMillis()
    logger.info(
      s"${request.request.protocol.value} ${request.request.method.value} " +
        s"${request.request.uri.path}?${request.request.uri.queryString().getOrElse("")}")

    (continue.route ~ suggestForNext.route ~ suggestForTheSame.route ~ advice.route ~ generate.route ~ suggest.route) (request)
      .map {
        case result: Complete =>
          // logger.debug(s"${result.response._3}")
          val tEnd = System.currentTimeMillis()
          logger.debug(s"${request.request.uri.path} ${tEnd - tStart} Completed")
          result
        case result => result
      }
  }
}


