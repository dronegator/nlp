package com.github.dronegator.nlp.component.twophrases

import com.github.dronegator.nlp.component.Component
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/19/16.
 */

object TwoPhrases {
  val Init = (List[Token](), Map[Token, Int]())

}
class TwoPhrases(cfgArg: CFG) extends Component[((List[Token], Map[Token, Int]), List[Token]), (List[Token], Map[Token, Int])] {
  override def cfg: CFG = cfgArg

  override def apply(in: ((List[Token], Map[Token, Int]), List[Token])): (List[Token], Map[Token, Int]) = {
    in match {
      case ((prev, map), next) =>
        val newmap = (prev.toSet & next.toSet).
          foldLeft(map){
            case (map, token) =>
              map + (token -> (map.getOrElse(token, 0) + 1))
        }
        (next, newmap)
    }
  }
}
