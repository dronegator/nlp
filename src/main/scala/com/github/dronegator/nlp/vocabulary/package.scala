package com.github.dronegator.nlp

import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Word, Token}

/**
 * Created by cray on 8/17/16.
 */
package object vocabulary {

  trait VocabularyRaw {
    def phrases: List[List[Token]]

    def ngrams1: Map[List[Token], Int]

    def ngrams2: Map[List[Token], Int]

    def ngrams3: Map[List[Token], Int]

    def toToken: Map[Word, List[Token]]

    def twoPhraseCorelator: Map[List[Token], Int]
  }

  trait Vocabulary extends VocabularyRaw {
    def toWord: Map[Token, Word]

    def vtoken: Map[List[Token], Double]

    def vngrams2: Map[List[Token], Double]

    def vngrams3: Map[List[Token], Double]

    def vnext1: Map[List[Token], List[(Double,Token)]]

    def vnext2: Map[List[Token], List[(Double,Token)]]

    def vpgrams2: Map[List[Token], Double]

    def vpgrams3: Map[List[Token], Double]

    def vprev1: Map[List[Token], List[(Double,Token)]]

    def vprev2: Map[List[Token], List[(Double,Token)]]

    def vmiddle: Map[List[Token], List[(Double, Token)]]

    def vcnext: Map[List[Token], List[(Double,Token)]]

  }
}
