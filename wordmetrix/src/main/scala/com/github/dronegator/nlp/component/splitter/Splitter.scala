package com.github.dronegator.nlp.component.splitter

import com.github.dronegator.nlp.component.ComponentMap
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Word

/**
 * Created by cray on 8/15/16.
 */
case class SplitterConfig(delimiter: String = """\b|\s""")
object Splitter {
}

class Splitter(cfg: SplitterConfig) extends ComponentMap[String, Iterator[Word]] {
  private val rSplit = cfg.delimiter.r

  override def apply(s: String): Iterator[Word] =
    rSplit.
      split(s).
      toIterator.
      map(_.trim).
      filter(_.nonEmpty)
}