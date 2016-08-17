package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.VocabularyRawImpl

/**
 * Created by cray on 8/17/16.
 */

object VocabularyImpl {
  implicit def apply(vocabulary: VocabularyRaw) =
    new VocabularyImpl(
      vocabulary.phrases,
      vocabulary.ngrams1,
      vocabulary.ngrams2,
      vocabulary.ngrams3,
      vocabulary.toToken
    )
}

class VocabularyImpl(phrases: List[List[Token]],
                     ngrams1: Map[List[Token], Int],
                     ngrams2: Map[List[Token], Int],
                     ngrams3: Map[List[Token], Int],
                     toToken: Map[Word, List[Token]]                      )
  extends VocabularyRawImpl(phrases, ngrams1, ngrams2, ngrams3, toToken) with Vocabulary {
  override lazy val toWord: Map[Token, Word] = ???

  override lazy val vngrams2: Map[List[Token], Double] = ???

  override lazy val vnext3: Map[List[Token], List[(Double,Token)]] = ???

  override lazy val vngrams1: Map[List[Token], Double] = ???

  override lazy val vnext2: Map[List[Token], List[(Double,Token)]] = ???

  override lazy val vnext1: Map[List[Token], List[(Double, Token)]] = ???

  override lazy val vngrams3: Map[List[Token], Double] = ???
}
