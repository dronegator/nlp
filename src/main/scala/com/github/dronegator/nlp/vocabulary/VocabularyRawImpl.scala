package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Word, Token}

/**
 * Created by cray on 8/17/16.
 */
object VocabularyRawImpl {
  implicit def apply(vocabulary: Vocabulary): VocabularyRawImpl =
    VocabularyRawImpl(
      vocabulary.phrases,
      vocabulary.ngrams1,
      vocabulary.ngrams2,
      vocabulary.ngrams3,
      vocabulary.toToken
    )
}

case class VocabularyRawImpl(phrases: List[List[Token]],
                             ngrams1: Map[List[Token], Int],
                             ngrams2: Map[List[Token], Int],
                             ngrams3: Map[List[Token], Int],
                             toToken: Map[Word, List[Token]]) extends VocabularyRaw
