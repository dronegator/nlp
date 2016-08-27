package com.github.dronegator.nlp

import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{TokenPreDef, Word, Token}
import com.github.dronegator.nlp.main.NLPTReplMain._

/**
 * Created by cray on 8/17/16.
 */
package object vocabulary {



  implicit class VocabularyTools(val vocabulary: Vocabulary) {
    def tokenize(s: String): List[Token] =
      {

        val tokens = splitter(s).
          scanLeft((vocabulary.toToken, 100000000, tokenizer.init._3))(tokenizer).
          map {
            case (_, _, tokens) => tokens
          }.
          toList :+ List(TokenPreDef.TEnd.value)

        val phrase = tokens.
          toIterator.
          scanLeft(accumulator.init)(accumulator).
          collectFirst {
            case (_, Some(phrase)) => phrase
          }.
          toList.
          flatten

        phrase
      }

    def untokenize(tokens: List[Token]) =
      tokens.flatMap(vocabulary.toWord.get(_)).mkString(" ")

    def probability(tokens: List[Token]) =
      tokens.
        sliding(3).
        map{
          case Nil =>
            1.0
          case tokens@(_ :: Nil) =>
            vocabulary.vtoken.get(tokens).getOrElse(0.0)

          case tokens@(_ :: _ :: Nil) =>
            vocabulary.vngrams2.get(tokens).getOrElse(0.0)

          case tokens=>
            vocabulary.vngrams3.get(tokens.take(3)).getOrElse(0.0)
        }.
        reduceOption(_*_).
        getOrElse(1.0)
  }

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
