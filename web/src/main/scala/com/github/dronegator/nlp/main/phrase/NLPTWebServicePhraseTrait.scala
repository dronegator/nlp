package com.github.dronegator.nlp.main.phrase

import com.github.dronegator.nlp.main.{NLPTApp, Concurent}
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import com.softwaremill.macwire._

/**
 * Created by cray on 9/12/16.
 */
object NLPTWebServicePhraseTrait {

}

trait NLPTWebServicePhraseTrait {
  this: NLPTApp with Concurent  =>

  lazy val continue = wire[ContinueHandler]

  lazy val suggestForNext = wire[SuggestNextHandler]

  lazy val suggestForTheSame = wire[SuggestForTheSameHandler]

  lazy val suggest = wire[SuggestHandler]

  lazy val generate = wire[GenerateHandler]

  lazy val advice = wire[AdviceHandler]
}

