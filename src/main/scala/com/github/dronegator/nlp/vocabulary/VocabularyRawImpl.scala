package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Statement, Word, Token}

/**
 * Created by cray on 8/17/16.
 */
object VocabularyRawImpl {
  implicit def apply(vocabulary: Vocabulary): VocabularyRawImpl =
    VocabularyRawImpl(
      vocabulary.phrases,
      vocabulary.nGram1,
      vocabulary.nGram2,
      vocabulary.nGram3,
      vocabulary.tokenMap,
      vocabulary.phraseCorrelationConsequent,
      vocabulary.phraseCorrelationInner
    )

}

case class VocabularyRawImpl(phrases: List[Statement],
                             nGram1: Map[List[Token], Int],
                             nGram2: Map[List[Token], Int],
                             nGram3: Map[List[Token], Int],
                             tokenMap: Map[Word, List[Token]],
                             phraseCorrelationConsequent: Map[List[Token], Int],
                             phraseCorrelationInner: Map[List[Token], Int])
  extends VocabularyRaw
