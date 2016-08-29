package com.github.dronegator.nlp.component.ngramscounter

import com.github.dronegator.nlp.component.{ComponentFold, ComponentState}
import com.github.dronegator.nlp.component.ngramscounter.NGramsCounter.Init
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/17/16.
 */
object NGramsCounter {
  type Init = Map[List[Token], Int]

  //TODO: We have to find a way to call cfg by name
  def factoryNGramsCounter1(cfgArg: CFG) =
    new NGramsCounter(cfgArg, 1)

  def factoryNGramsCounter2(cfgArg:  CFG) =
    new NGramsCounter(cfgArg, 2)

  def factoryNGramsCounter3(cfgArg: CFG) =
    new NGramsCounter(cfgArg, 3)
}

class NGramsCounter(cfgArg: => CFG, n: Int) extends ComponentFold[List[Token], Init, Init] {
  override def cfg: CFG = cfgArg

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
