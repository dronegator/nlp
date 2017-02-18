package com.github.dronegator.nlp.vocabulary.ToolMiniLanguage

/**
  * Created by cray on 9/22/16.
  */


import akka.stream.scaladsl._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.OtherWord
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyRaw, VocabularyRawImpl}
import com.typesafe.scalalogging.LazyLogging

object ToolMiniLanguageTrait extends LazyLogging {

}

trait ToolMiniLanguageTrait {
  //this: VocabularyTools =>

  def vocabulary: Vocabulary

  def miniLanguage(keywords: Seq[Token]): VocabularyRaw = {
    val renumerator = keywords.toList.sorted.partition(_ < 10) match {
      case (_, ks2) =>
        val map = ((1 until 10).map(x => x -> x) ++ ks2.zipWithIndex.map(x => x._1 -> (x._2 + 10))).toMap
        x: Token => map.getOrElse(x, OtherWord.value): Token
    }

    val doesKeywordExist = keywords.toSet

    VocabularyRawImpl(
      tokenMap = vocabulary.tokenMap
        .map {
          case (word, tokens) =>
            (word, tokens.filter(keywords contains _).map(renumerator))
        }
        .filter(_._2.nonEmpty),
      meaningMap = vocabulary.meaningMap
        .collect {
          case ((t1, t2), ps) if (keywords contains t1) && (keywords contains t2) =>
            ((renumerator(t1), renumerator(t2)), ps)
        },
      statements = vocabulary.statements
        .filterNot(_.exists(!keywords.contains(_)))
        .filter(_.length > 4)
        .distinct
        .map {
          _.map(renumerator)
        },
      nGram1 = vocabulary.nGram1
        .collect {
          case (tokens, p) if tokens.forall(doesKeywordExist(_)) =>
            (tokens.map(renumerator), p)
        },
      nGram2 = vocabulary.nGram2
        .collect {
          case (tokens, p) if tokens.forall(doesKeywordExist(_)) =>
            (tokens.map(renumerator), p)
        },
      nGram3 = vocabulary.nGram3
        .collect {
          case (tokens, p) if tokens.forall(doesKeywordExist(_)) =>
            (tokens.map(renumerator), p)
        },
      phraseCorrelationRepeated = vocabulary.phraseCorrelationRepeated
        .filter(x => keywords.contains(x._1))
        .map { case (x, y) => (renumerator(x), y) },
      phraseCorrelationConsequent = vocabulary.phraseCorrelationConsequent
        .filterNot(x => x._1.exists(!keywords.contains(_)))
        .map { case (xs, y) => (xs.map(renumerator(_)), y) },
      phraseCorrelationInner = vocabulary.phraseCorrelationInner
        .filterNot(x => x._1.exists(!keywords.contains(_)))
        .map { case (xs, y) => (xs.map(renumerator(_)), y) }
    )
  }

  def miniLanguageKeywords(tokens: Set[Token]) = {
    val adviceFlow = AdviceFlow(vocabulary, tokens)

    val traversalComponent = TraversalComponent(adviceFlow)

    Source.fromIterator(() => tokens.toIterator)
      .via(traversalComponent)
  }
}
