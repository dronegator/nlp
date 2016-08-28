package com.github.dronegator.nlp

import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenPreDef, Word}
import com.github.dronegator.nlp.main.NLPTReplMain._

/**
 * Created by cray on 8/17/16.
 */
package object vocabulary {

  implicit class VocabularyTools(val vocabulary: Vocabulary) {
    def tokenize(s: String): List[Token] = {

      val tokens = splitterTool(s).
        scanLeft((vocabulary.tokenMap, 100000000, tokenizerTool.init._3))(tokenizerTool).
        map {
          case (_, _, tokens) => tokens
        }.
        toList :+ List(TokenPreDef.TEnd.value)

      val phrase = tokens.
        toIterator.
        scanLeft(accumulatorTool.init)(accumulatorTool).
        collectFirst {
          case (_, Some(phrase)) => phrase
        }.
        toList.
        flatten

      phrase
    }

    def untokenize(tokens: List[Token]) =
      tokens.flatMap(vocabulary.wordMap.get(_)).mkString(" ")

    def probability(tokens: List[Token]) =
      tokens.
        sliding(3).
        map {
          case Nil =>
            1.0
          case tokens@(_ :: Nil) =>
            vocabulary.pToken.get(tokens).getOrElse(0.0)

          case tokens@(_ :: _ :: Nil) =>
            vocabulary.pNGram2.get(tokens).getOrElse(0.0)

          case tokens =>
            vocabulary.pNGram3.get(tokens.take(3)).getOrElse(0.0)
        }.
        reduceOption(_ * _).
        getOrElse(1.0)
  }

  trait VocabularyRaw {
    def phrases: List[List[Token]]

    def nGram1: Map[List[Token], Int]

    def nGram2: Map[List[Token], Int]

    def nGram3: Map[List[Token], Int]

    def tokenMap: Map[Word, List[Token]]

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
