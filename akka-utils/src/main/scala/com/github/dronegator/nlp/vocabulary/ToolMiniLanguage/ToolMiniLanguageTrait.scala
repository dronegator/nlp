package com.github.dronegator.nlp.vocabulary.ToolMiniLanguage

/**
  * Created by cray on 9/22/16.
  */


import akka.stream.scaladsl._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyRaw, VocabularyRawImpl}
import com.typesafe.scalalogging.LazyLogging

object ToolMiniLanguageTrait extends LazyLogging {

}

trait ToolMiniLanguageTrait {
  //this: VocabularyTools =>

  def vocabulary: Vocabulary

  def miniLanguage(keywords: Set[Token]): VocabularyRaw =
    VocabularyRawImpl(
      tokenMap = vocabulary.tokenMap
        .map {
          case (word, tokens) =>
            (word, tokens.filter(keywords contains _))
        }
        .filter(_._2.nonEmpty),
      meaningMap = vocabulary.meaningMap
        .filter {
          case ((t1, t2), _) =>
            (keywords contains t1) && (keywords contains t2)
        },
      statements = vocabulary.statements
        .filterNot(_.exists(!keywords.contains(_)))
        .filter(_.length > 4)
        .distinct,
      nGram1 = vocabulary.nGram1
        .filterNot {
          case (tokens, _) =>
            tokens.exists(!keywords(_))
        },
      nGram2 = vocabulary.nGram2
        .filterNot {
          case (tokens, _) =>
            tokens.exists(!keywords(_))
        },
      nGram3 = vocabulary.nGram3
        .filterNot {
          case (tokens, _) =>
            tokens.exists(!keywords(_))
        },
      phraseCorrelationRepeated = vocabulary.phraseCorrelationRepeated
        .filter(x => keywords.contains(x._1)),
      phraseCorrelationConsequent = vocabulary.phraseCorrelationConsequent
        .filterNot(x => x._1.exists(!keywords.contains(_))),
      phraseCorrelationInner = vocabulary.phraseCorrelationInner
        .filterNot(x => x._1.exists(!keywords.contains(_)))
    )

  def miniLanguageKeywords(tokens: Set[Token]) = {
    val adviceFlow = AdviceFlow(vocabulary, tokens)

    val traversalComponent = TraversalComponent(adviceFlow)

    Source.fromIterator(() => tokens.toIterator)
      .via(traversalComponent)
  }
}
