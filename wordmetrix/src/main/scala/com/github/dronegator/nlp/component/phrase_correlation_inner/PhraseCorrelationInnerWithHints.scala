package com.github.dronegator.nlp.component.phrase_correlation_repeated

import com.github.dronegator.nlp.component.ComponentFold
import com.github.dronegator.nlp.component.phrase_correlation_repeated.PhraseCorrelationInnerWithHints.Init
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.vocabulary.VocabularyHint
import com.github.dronegator.nlp.vocabulary.VocabularyTools.VocabularyHintTools

/**
 * Created by cray on 8/19/16.
 *
 * Calculates correlation between tokens in a statement
 */
case class PhraseCorrelationInnerWithHintsConfig()

object PhraseCorrelationInnerWithHints {
  type Init = (Map[ List[Token], Int])
}

class PhraseCorrelationInnerWithHints(cfgArg: PhraseCorrelationInnerWithHintsConfig, vocabularyHint: VocabularyHint) extends ComponentFold[List[Token], Init, Init] {

  private val toWord =
    vocabularyHint.tokenMap.flatMap{
      case (word, tokens) =>
        tokens.map{
          case (token) =>
            token -> word
        }
    }.withDefaultValue("unknown")

  override def init: Init= (Map[List[Token], Int]())

  override def apply(state: Init, statement: List[Token]): Init = {
    state match {
      case (map) =>
        val statementSens = vocabularyHint.keywords(statement).collect{
          case (token, (p, _, _)) if p > 0 =>
           // println(s"${toWord(token)} $p")
            token
        }

        (for {
          token1 <- statementSens
          token2 <- statementSens if token1 != token2
        }
          yield (token1 :: token2 :: Nil)).
          foldLeft(map) {
            case (map, pair) =>
              map + (pair-> (map.getOrElse(pair, 0) + 1))
          }
    }
  }

  override val select: Select = {
    case x => x
  }
}
