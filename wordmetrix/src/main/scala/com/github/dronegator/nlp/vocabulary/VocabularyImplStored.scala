package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.common.Probability
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, Word}

/**
  * Created by cray on 9/18/16.
  */

object VocabularyImplStored {
  def apply(vocabulary: Vocabulary) =
    new VocabularyImplStored(
      vocabulary.wordMap,
      vocabulary.pToken.map(identity),
      vocabulary.pNGram2,
      vocabulary.pNGram3,
      vocabulary.map1ToNext,
      vocabulary.map2ToNext,
      vocabulary.map1ToPrev,
      vocabulary.map2ToPrev,
      vocabulary.map2ToMiddle,
      vocabulary.map1ToNextPhrase,
      vocabulary.map1ToTheSamePhrase,
      vocabulary.statements,
      vocabulary.nGram1,
      vocabulary.nGram2,
      vocabulary.nGram3,
      vocabulary.tokenMap,
      vocabulary.phraseCorrelationRepeated,
      vocabulary.phraseCorrelationConsequent,
      vocabulary.phraseCorrelationInner,
      vocabulary.meaningMap,
      vocabulary.sense,
      vocabulary.auxiliary
    )
}

case class VocabularyImplStored(wordMap: Map[Token, Word],
                                pToken: Map[List[Token], Double],
                                pNGram2: Map[List[Token], Double],
                                pNGram3: Map[List[Token], Double],
                                map1ToNext: Map[List[Token], List[(Double, Token)]],
                                map2ToNext: Map[List[Token], List[(Double, Token)]],
                                map1ToPrev: Map[List[Token], List[(Double, Token)]],
                                map2ToPrev: Map[List[Token], List[(Double, Token)]],
                                map2ToMiddle: Map[List[Token], List[(Double, Token)]],
                                map1ToNextPhrase: Map[Token, List[(Token, Double)]],
                                map1ToTheSamePhrase: Map[Token, List[(Token, Probability)]],
                                statements: List[List[Token]],
                                nGram1: Map[List[Token], Int],
                                nGram2: Map[List[Token], Int],
                                nGram3: Map[List[Token], Int],
                                tokenMap: Map[Word, List[Token]],
                                phraseCorrelationRepeated: Map[Token, Int],
                                phraseCorrelationConsequent: Map[List[Token], Int],
                                phraseCorrelationInner: Map[List[Token], Int],
                                meaningMap: Map[(Token, Token), (Probability, Probability)],
                                sense: Set[Token],
                                auxiliary: Set[Token])
  extends Vocabulary
    with VocabularyImplTrait
