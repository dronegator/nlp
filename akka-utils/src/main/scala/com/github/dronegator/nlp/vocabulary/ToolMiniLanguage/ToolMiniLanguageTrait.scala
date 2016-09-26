package com.github.dronegator.nlp.vocabulary.ToolMiniLanguage

/**
 * Created by cray on 9/22/16.
 */


import akka.stream.scaladsl._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.Vocabulary
import com.typesafe.scalalogging.LazyLogging

object ToolMiniLanguageTrait extends LazyLogging {

}

trait ToolMiniLanguageTrait {
  //this: VocabularyTools =>

  def vocabulary: Vocabulary

  def miniLanguage(keywords: Set[Token]): Vocabulary = ???

  def miniLanguageKeywords(tokens: Set[Token]) = {
    val adviceFlow = AdviceFlow(vocabulary, tokens)

    val traversalComponent = TraversalComponent(adviceFlow)

    Source.fromIterator(() => tokens.toIterator)
      .via(traversalComponent)
  }
}
