package com.github.dronegator.nlp.component.splitter

import com.github.dronegator.nlp.component.ComponentMap
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Word
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/15/16.
 */
object Splitter {
}

class Splitter(cfgArg: => CFG) extends ComponentMap[String, Iterator[Word]] {
  private val rSplit = """\b|\s""".r

  def cfg = cfgArg

  override def apply(s: String): Iterator[Word] =
    rSplit.
      split(s).
      toIterator.
      map(_.trim).
      filter(_.nonEmpty)
}