package com.github.dronegator.nlp.component

/**
 * Created by cray on 8/19/16.
 */
import com.github.dronegator.nlp.component.Component
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.utils.CFG

object TwoPhraseCorelator {
  val Init = (List[Token](), Map[List[Token], Int]())

}
class TwoPhraseCorelator(cfgArg: CFG) extends Component[((List[Token], Map[List[Token], Int]), List[Token]), (List[Token], Map[List[Token], Int])] {
  override def cfg: CFG = cfgArg

  override def apply(in: ((List[Token], Map[List[Token], Token]), List[Token])): (List[Token], Map[List[Token], Token]) =
    in match {
      case ((prev, map), next) =>
        val newmap = (for {
          p <- prev
          n <- next
        } yield (p :: n :: Nil)).
          foldLeft(map){
            case (map, pair) =>
              map + (pair -> (map.getOrElse(pair, 0) + 1))
          }
        (next, newmap)
    }
}