package com.github.dronegator.nlp.main.chain

import com.github.dronegator.nlp.common.Probability
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, Word}
import com.github.dronegator.nlp.vocabulary.VocabularyRaw

/**
  * Created by cray on 2/4/17.
  * rm test.dat; sbt "ml/runMain com.github.dronegator.nlp.main.chain.NNChainMainImpl Test --algorithm=SimpleSGD --crossvalidation-ratio=0 --n-token=20 --n-klassen=2 --range=1.0 test.dat"  |less -r
  */

object TestVocabulary {
  val Sample = List(
    11 :: 11 :: 12 :: Nil,
    11 :: 11 :: 13 :: Nil,
    11 :: 12 :: 14 :: Nil,
    11 :: 12 :: 13 :: Nil,
    11 :: 13 :: 14 :: Nil,
    11 :: 13 :: 12 :: Nil,
    12 :: 11 :: 14 :: Nil,
    12 :: 11 :: 13 :: Nil,
    12 :: 12 :: 11 :: Nil,
    12 :: 12 :: 13 :: Nil,
    12 :: 13 :: 11 :: Nil,
    12 :: 13 :: 14 :: Nil,
    13 :: 11 :: 12 :: Nil,
    13 :: 11 :: 14 :: Nil,
    13 :: 12 :: 11 :: Nil,
    13 :: 12 :: 14 :: Nil,
    13 :: 13 :: 11 :: Nil,
    13 :: 13 :: 12 :: Nil
  )
}

class TestVocabulary extends VocabularyRaw {

  import TestVocabulary._

  override def statements: List[List[Token]] = Nil

  override def nGram1: Map[List[Token], Int] =
    ((0 to 10) ++ Sample
      .flatten
      .distinct)
      .map(x => (x :: Nil) -> 1)
      .toMap

  override def nGram2: Map[List[Token], Int] =
    Sample
      .map {
        case x :: y :: _ =>
          (x :: y :: Nil) -> 1
      }
      .distinct
      .toMap

  override def nGram3: Map[List[Token], Int] =
    Sample
      .map(_ -> 1)
      .toMap

  override def tokenMap: Map[Word, List[Token]] =
    Sample
      .flatten
      .distinct
      .map(x => s"$x" -> (x :: Nil))
      .toMap


  override def phraseCorrelationRepeated: Map[Token, Int] = Map()

  override def phraseCorrelationConsequent: Map[List[Token], Int] = Map()

  override def phraseCorrelationInner: Map[List[Token], Int] = Map()

  override def meaningMap: Map[(Token, Token), (Probability, Probability)] = Map()

  override def sense: Set[Token] = ???

  override def auxiliary: Set[Token] = ???
}
