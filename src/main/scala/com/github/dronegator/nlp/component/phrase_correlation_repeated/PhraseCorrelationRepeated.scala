package com.github.dronegator.nlp.component.phrase_correlation_repeated

import com.github.dronegator.nlp.component.ComponentFold
import com.github.dronegator.nlp.component.phrase_correlation_repeated.PhraseCorrelationRepeated.Init
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/19/16.
 *
 * Calculates correlation between tokens in two consequent phrases
 */

object PhraseCorrelationRepeated {
  type Init = (List[Token], Map[Token, Int])

}

class PhraseCorrelationRepeated(cfgArg: CFG) extends ComponentFold[List[Token], Init] {
  override def cfg: CFG = cfgArg

  override def init: Init = (List[Token](), Map[Token, Int]())

  override def apply(state: Init, phrase: List[Token]): (List[Token], Map[Token, Token]) = {
    state match {
      case (prev, map) =>
        val newmap = (prev.toSet & phrase.toSet).
          foldLeft(map) {
            case (map, token) =>
              map + (token -> (map.getOrElse(token, 0) + 1))
          }
        (phrase, newmap)
    }
  }
}
