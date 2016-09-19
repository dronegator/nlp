package com.github.dronegator.nlp.main.system

/**
  * Created by cray on 9/12/16.
  */

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.dronegator.nlp.main.{Concurent, NLPTAppForWeb}
import com.softwaremill.macwire._

object NLPTWebServiceSystemTrait {

}

trait NLPTWebServiceSystemTrait
  extends
    NLPTAppForWeb {
  this: Concurent =>

  lazy val version: NTLPWebSystemVersionHandler = wire[NTLPWebSystemVersionHandler]

  lazy val vocabularyStat: NTLPWebSystemVocabularyStatHandler = wire[NTLPWebSystemVocabularyStatHandler]

  abstract override def route: Route = pathPrefix("system") { request =>
    (version.route ~ vocabularyStat.route) (request)
  } ~ super.route
}


