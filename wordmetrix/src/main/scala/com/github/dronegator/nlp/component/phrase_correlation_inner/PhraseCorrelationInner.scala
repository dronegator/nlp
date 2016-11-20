package com.github.dronegator.nlp.component.phrase_correlation_repeated

import com.github.dronegator.nlp.component.ComponentFold
import com.github.dronegator.nlp.component.phrase_correlation_repeated.PhraseCorrelationInner.Init
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token

/**
 * Created by cray on 8/19/16.
 *
 * Calculates correlation between tokens in a statement
 */


case class PhraseCorrelationInnerConfig()
object PhraseCorrelationInner {
  type Init = (Map[ List[Token], Int])
}

class PhraseCorrelationInner(cfg: PhraseCorrelationInnerConfig) extends ComponentFold[List[Token], Init, Init] {

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
