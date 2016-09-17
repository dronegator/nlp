package com.github.dronegator.nlp.main.system
/**
  * Created by cray on 9/12/16.
  */

import com.github.dronegator.nlp.main.{NLPTApp, Concurent}
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import com.softwaremill.macwire._

object NLPTWebServiceSystemTrait {

}

trait NLPTWebServiceSystemTrait {
  this: NLPTApp with Concurent  =>

  lazy val version = wire[NTLPWebSystemVersionHandler]

  lazy val vocabularyStat = wire[NTLPWebSystemVocabularyStatHandler]


}


