package com.github.dronegator.nlp.main.phrase

import com.github.dronegator.nlp.main.Concurent
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import com.softwaremill.macwire._

/**
 * Created by cray on 9/12/16.
 */
object NLPTWebServicePhraseTrait {

}

trait NLPTWebServicePhraseTrait {
  this: Concurent =>

  def vocabulary: VocabularyImpl

  lazy val continue = wire[ContinueHandler]

  lazy val suggestForNext = wire[SuggestNextHandler]

  lazy val suggestForTheSame = wire[SuggestForTheSameHandler]

  lazy val suggest = wire[SuggestHandler]
}


