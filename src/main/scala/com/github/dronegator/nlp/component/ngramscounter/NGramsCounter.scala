package com.github.dronegator.nlp.component.ngramscounter

import com.github.dronegator.nlp.component.ComponentFold
import com.github.dronegator.nlp.component.ngramscounter.NGramsCounter.Init
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/17/16.
 */
object NGramsCounter {
  type Init = Map[List[Token], Int]
}

class NGramsCounter(cfgArg: => CFG, n: Int) extends ComponentFold[List[Token], Init] {
  override def cfg: CFG = cfgArg

  override def init: Init = Map[List[Token], Int]()

  def apply(map: Map[List[Token], Int], phrase: List[Token]): Map[List[Token], Int] =
    phrase.sliding(n).foldLeft(map) {
      case (map, token) if token.length == n => map + (token -> (1 + map.getOrElse(token, 0)))
      case (map, _) => map
    }


}
