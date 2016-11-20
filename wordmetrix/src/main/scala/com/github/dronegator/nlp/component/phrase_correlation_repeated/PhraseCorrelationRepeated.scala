package com.github.dronegator.nlp.component.phrase_correlation_repeated

import com.github.dronegator.nlp.component.ComponentFold
import com.github.dronegator.nlp.component.phrase_correlation_repeated.PhraseCorrelationRepeated.Init
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token

/**
 * Created by cray on 8/19/16.
 *
 * Calculates correlation between tokens in two consequent phrases
 */

case class PhraseCorrelationRepeatedConfig()

object PhraseCorrelationRepeated {
  type Init = (List[Token], Map[Token, Int])

}

class PhraseCorrelationRepeated(cfg: PhraseCorrelationRepeatedConfig) extends ComponentFold[List[Token], Init, Map[Token, Int]] {

  override def init: Init = (List[Token](), Map[Token, Int]())

  override def apply(state: Init, statement: List[Token]): (List[Token], Map[Token, Token]) = {
    state match {
      case (prev, map) =>
        val newmap = (prev.toSet & statement.toSet).
          foldLeft(map) {
            case (map, token) =>
              map + (token -> (map.getOrElse(token, 0) + 1))
          }
        (statement, newmap)
    }
  }

  override val select: Select = {
    case (_, map) =>
      map
  }
}
