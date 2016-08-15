package com.github.dronegator.nlp.component.splitter

import com.github.dronegator.nlp.component.Component
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Word
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/15/16.
 */
object Splitter {
}

class Splitter(cfgArg: => CFG) extends Component[String, List[Word]] {
  private val rSplit = """\s+""".r

  def cfg = cfgArg

  override def apply(s: String): List[Word] = {
    rSplit.split(s)
  }.toList
}