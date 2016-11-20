package com.github.dronegator.nlp.component.ngramscounter

import com.github.dronegator.nlp.component.ComponentFold
import com.github.dronegator.nlp.component.ngramscounter.NGramsCounter.Init
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token

/**
 * Created by cray on 8/17/16.
 */

case class NGramsCounterConfig()
object NGramsCounter {
  type Init = Map[List[Token], Int]

  //TODO: We have to find a way to call cfg by name
  def factoryNGramsCounter1(cfg: NGramsCounterConfig) =
  new NGramsCounter(cfg, 1)

  def factoryNGramsCounter2(cfg: NGramsCounterConfig) =
    new NGramsCounter(cfg, 2)

  def factoryNGramsCounter3(cfg: NGramsCounterConfig) =
    new NGramsCounter(cfg, 3)
}

class NGramsCounter(cfg: NGramsCounterConfig, n: Int) extends ComponentFold[List[Token], Init, Init] {

  override def init: Init = Map[List[Token], Int]()

  def apply(map: Map[List[Token], Int], statement: List[Token]): Map[List[Token], Int] =
    statement.sliding(n).foldLeft(map) {
      case (map, token) if token.length == n => map + (token -> (1 + map.getOrElse(token, 0)))
      case (map, _) => map
    }

  override val select: Select = {
    case x => x
  }
}
