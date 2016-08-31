package com.github.dronegator.nlp

import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenPreDef, Word}

/**
 * Created by cray on 8/17/16.
 */
package object vocabulary {

  // implicit class VocabularyToolsExt(val vocabulary: Vocabulary) extends VocabularyTools.VocabularyTools(vocabulary)

  trait VocabularyRaw {
    def phrases: List[List[Token]]

    def nGram1: Map[List[Token], Int]

    def nGram2: Map[List[Token], Int]

    def nGram3: Map[List[Token], Int]

    def tokenMap: Map[Word, List[Token]]

    def phraseCorrelationRepeated: Map[Token, Int]

    def phraseCorrelationConsequent: Map[List[Token], Int]

    def phraseCorrelationInner: Map[List[Token], Int]
  }

  trait Vocabulary extends VocabularyRaw {
    def wordMap: Map[Token, Word]

    def pToken: Map[List[Token], Double]

    def pNGram2: Map[List[Token], Double]

    def pNGram3: Map[List[Token], Double]

    def map1ToNext: Map[List[Token], List[(Double, Token)]]

    def map2ToNext: Map[List[Token], List[(Double, Token)]]

    protected def pNGram2Prev: Map[List[Token], Double]

    protected def pNGram3Prev: Map[List[Token], Double]

    def map1ToPrev: Map[List[Token], List[(Double, Token)]]

    def map2ToPrev: Map[List[Token], List[(Double, Token)]]

    def map2ToMiddle: Map[List[Token], List[(Double, Token)]]

    def map1ToNextPhrase: Map[List[Token], List[(Double, Token)]]
  }
}
