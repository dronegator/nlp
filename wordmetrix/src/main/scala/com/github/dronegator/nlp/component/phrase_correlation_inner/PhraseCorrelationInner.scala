package com.github.dronegator.nlp.component.phrase_correlation_repeated

import com.github.dronegator.nlp.component.{ComponentFold, ComponentState}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.component.phrase_correlation_repeated.PhraseCorrelationInner.Init
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/19/16.
 *
 * Calculates correlation between tokens in a statement
 */

object PhraseCorrelationInner {
  type Init = (Map[ List[Token], Int])
}

class PhraseCorrelationInner(cfgArg: CFG) extends ComponentFold[List[Token], Init, Init] {
  override def cfg: CFG = cfgArg

  override def init: Init= (Map[List[Token], Int]())

  override def apply(state: Init, statement: List[Token]): Init = {
    state match {
      case (map) =>
        (for {
          token1 <- statement
          token2 <- statement if token1 != token2
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
