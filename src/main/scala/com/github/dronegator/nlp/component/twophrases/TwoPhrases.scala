package com.github.dronegator.nlp.component.twophrases

import com.github.dronegator.nlp.component.ComponentFold
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.component.twophrases.TwoPhrases.Init
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/19/16.
 */

object TwoPhrases {
  type Init = (List[Token], Map[Token, Int])

}

class TwoPhrases(cfgArg: CFG) extends ComponentFold[List[Token], Init] {
  override def cfg: CFG = cfgArg

  override def init: (List[Token], Map[Token, Token]) = (List[Token](), Map[Token, Int]())

  override def apply(state: (List[Token], Map[Token, Token]), phrase: List[Token]): (List[Token], Map[Token, Token]) = {
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
