package com.github.dronegator.nlp.ml.vocabulary

import com.github.dronegator.nlp.common.Probability
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Statement, Token, Word}
import com.github.dronegator.nlp.main.chain.NNChainWithConstImpl
import com.github.dronegator.nlp.main.keyword.NNKeywordYesNoImpl
import com.github.dronegator.nlp.vocabulary.Vocabulary

/**
  * Created by cray on 3/9/17.
  */
case class VocabularyNeural(vocabulary: Vocabulary, nnKeyword: NNKeywordYesNoImpl, nnChain: NNChainWithConstImpl)
  extends Vocabulary {

  lazy val map2ToNext: Map[List[Tokenizer.Token], List[(Double, Tokenizer.Token)]] =
    new Map2ToNext(nnChain)

  lazy val meaningMap: Map[(Token, Token), (Probability, Probability)] =
    new MeaningMap(nnKeyword)

  override def wordMap = vocabulary.wordMap

  override def pToken: Map[List[Token], Double] =
    vocabulary.pToken

  override def pNGram2: Map[List[Token], Double] =
    vocabulary.pNGram2

  override def pNGram3: Map[List[Token], Double] =
    vocabulary.pNGram3

  override def map1ToNext: Map[List[Token], List[(Double, Token)]] =
    vocabulary.map1ToNext

  override def map1ToPrev: Map[List[Token], List[(Double, Token)]] =
    vocabulary.map1ToPrev

  override def map2ToPrev: Map[List[Token], List[(Double, Token)]] =
    vocabulary.map2ToPrev

  override def map2ToMiddle: Map[List[Token], List[(Double, Token)]] =
    vocabulary.map2ToMiddle

  override def map1ToNextPhrase: Map[Token, List[(Token, Double)]] =
    vocabulary.map1ToNextPhrase

  override def map1ToTheSamePhrase: Map[Token, List[(Token, Probability)]] =
    vocabulary.map1ToTheSamePhrase

  override def statementDenominator(statement: Statement): Double =
    vocabulary.statementDenominator(statement)

  override def statements: List[List[Token]] =
    vocabulary.statements

  override def nGram1: Map[List[Token], Int] =
    vocabulary.nGram1

  override def nGram2: Map[List[Token], Int] =
    vocabulary.nGram2

  override def nGram3: Map[List[Token], Int] =
    vocabulary.nGram3

  override def tokenMap: Map[Word, List[Token]] =
    vocabulary.tokenMap

  override def phraseCorrelationRepeated: Map[Token, Int] =
    vocabulary.phraseCorrelationRepeated

  override def phraseCorrelationConsequent: Map[List[Token], Int] =
    vocabulary.phraseCorrelationConsequent

  override def phraseCorrelationInner: Map[List[Token], Token] =
    vocabulary.phraseCorrelationInner

  override def sense: Set[Token] =
    vocabulary.sense

  override def auxiliary: Set[Token] =
    vocabulary.auxiliary


}

