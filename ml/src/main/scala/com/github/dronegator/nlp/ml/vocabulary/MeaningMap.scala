package com.github.dronegator.nlp.ml.vocabulary

import com.github.dronegator.nlp.common._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.main.keyword.NNKeywordYesNoImpl

/**
  * Created by cray on 3/9/17.
  */
class MeaningMap(nnKeyword: NNKeywordYesNoImpl)
  extends Map[(Token, Token), (Probability, Probability)] {
  override def empty: MeaningMap = this

  override def get(key: (Token, Token)): Option[(Probability, Probability)] = {
    key match {
      case (before, after) =>
        val probability = nnKeyword((before, after))(0)
        Some((probability, 1 - probability))

      case _ =>
        None
    }
  }

  override def iterator: Iterator[((Token, Token), (Probability, Probability))] = ???

  override def +[B1 >: (Probability, Probability)](kv: ((Token, Token), B1)): Map[(Token, Token), B1] = ???

  override def -(key: (Token, Token)): MeaningMap = ???
}
