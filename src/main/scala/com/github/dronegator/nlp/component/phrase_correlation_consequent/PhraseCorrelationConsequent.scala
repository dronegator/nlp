package com.github.dronegator.nlp.component.phrase_correlation_consequent

/**
 * Created by cray on 8/19/16.
 */

import com.github.dronegator.nlp.component.ComponentFold
import com.github.dronegator.nlp.component.phrase_correlation_consequent.PhraseCorrelationConsequent.Init
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.utils.CFG

object PhraseCorrelationConsequent {
  type Init = (List[Token], Map[List[Token], Int])
}

class PhraseCorrelationConsequent(cfgArg: CFG) extends ComponentFold[List[Token], Init] {
  override def cfg: CFG = cfgArg

  override def init = (List[Token](), Map[List[Token], Int]())

  override def apply(state: Init, phrase: List[Token]): (List[Token], Map[List[Token], Token]) =
    state match {
      case (prev, map) =>
        val newmap = (for {
          p <- prev
          n <- phrase
        } yield (p :: n :: Nil)).
          foldLeft(map) {
            case (map, pair) =>
              map + (pair -> (map.getOrElse(pair, 0) + 1))
          }
        (phrase, newmap)
    }

}