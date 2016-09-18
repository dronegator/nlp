package com.github.dronegator.nlp.main.phrase

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult.Complete
import com.github.dronegator.nlp.main.{Concurent, NLPTApp, NLPTAppForWeb}
import com.softwaremill.macwire._

/**
 * Created by cray on 9/12/16.
 */
object NLPTWebServicePhraseTrait {

}

trait NLPTWebServicePhraseTrait
  extends NLPTAppForWeb {
  this: Concurent  =>

  lazy val continue: ContinueHandler = wire[ContinueHandler]

  lazy val suggestForNext: SuggestNextHandler = wire[SuggestNextHandler]

  lazy val suggestForTheSame: SuggestForTheSameHandler = wire[SuggestForTheSameHandler]

  lazy val suggest: SuggestHandler = wire[SuggestHandler]

  lazy val generate: GenerateHandler = wire[GenerateHandler]

  lazy val advice: AdviceHandler = wire[AdviceHandler]

  abstract override def route: Route  = pathPrefix("phrase") { request =>
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
  } ~ super.route
}


