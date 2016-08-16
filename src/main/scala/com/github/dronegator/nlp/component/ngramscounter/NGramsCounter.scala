package com.github.dronegator.nlp.component.ngramscounter

import com.github.dronegator.nlp.component.Component
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/17/16.
 */
object NGramsCounter {

}

class NGramsCounter(cfgArg: => CFG, n: Int) extends Component[(Map[List[Token], Int], List[Token]), Map[List[Token], Int]] {
  override def cfg: CFG = cfgArg

  override def apply(in: (Map[List[Token], Token], List[Token])): Map[List[Token], Token] = in match {
    case (map, phrase) =>
      phrase.sliding(n).foldLeft(map) {
        case (map, token) if token.length == n => map + (token -> (1 + map.getOrElse(token, 0)))
        case (map, _) => map
      }
  }
}
