package com.github.dronegator.nlp.main.system
/**
  * Created by cray on 9/12/16.
  */

import akka.http.scaladsl.server.Directives._
import com.github.dronegator.nlp.main.NLPTWebServiceMain._
import com.github.dronegator.nlp.main.{Concurent, NLPTApp}
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import com.softwaremill.macwire._

object NLPTWebServiceSystemTrait {

}

trait NLPTWebServiceSystemTrait {
  this: NLPTApp with Concurent  =>

  lazy val version: NTLPWebSystemVersionHandler = wire[NTLPWebSystemVersionHandler]

  lazy val vocabularyStat: NTLPWebSystemVocabularyStatHandler = wire[NTLPWebSystemVocabularyStatHandler]

  lazy val routeSystem = pathPrefix("system") { request =>
    (version.route ~ vocabularyStat.route) (request)
  }
}


