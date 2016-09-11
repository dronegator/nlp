package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.common._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Statement, Word, Token}

/**
 * Created by cray on 8/17/16.
 */
object VocabularyRawImpl {
  implicit def apply(vocabulary: Vocabulary): VocabularyRaw =
    VocabularyRawImpl(
      vocabulary.tokenMap,
      vocabulary.meaningMap,
      vocabulary.statements,
      vocabulary.nGram1,
      vocabulary.nGram2,
      vocabulary.nGram3,
      vocabulary.phraseCorrelationRepeated,
      vocabulary.phraseCorrelationConsequent,
      vocabulary.phraseCorrelationInner
    )
}

case class VocabularyRawImpl(tokenMap: Map[Word, List[Token]],
                             meaningMap: Map[(Token, Token), (Probability, Probability)] ,
                             statements: List[Statement],
                             nGram1: Map[List[Token], Int],
                             nGram2: Map[List[Token], Int],
                             nGram3: Map[List[Token], Int],
                             phraseCorrelationRepeated: Map[Token, Int],
                             phraseCorrelationConsequent: Map[List[Token], Int],
                             phraseCorrelationInner: Map[List[Token], Int])
  extends VocabularyRaw
  with VocabularyHintWords
