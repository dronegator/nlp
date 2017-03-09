package com.github.dronegator.nlp.ml.vocabulary

import com.github.dronegator.nlp.common._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.main.chain.NNChainWithConstImpl

/**
  * Created by cray on 3/9/17.
  */
class Map2ToNext(nnChain: NNChainWithConstImpl)
  extends Map[List[Token], List[(Double, Token)]] {

  override def get(key: List[Token]): Option[List[(Probability, Token)]] = {
    key match {
      case t1 :: t2 :: _ =>
        val output = nnChain((t1, t2)).activeIterator
          .map {
            case (x, y) =>
              (y, x)
          }
          .toList
          .sortBy(_._1)

        Some(output)

      case _ =>
        None
    }
  }

  override def +[B1 >: List[(Probability, Token)]](kv: (List[Token], B1)): Map[List[Token], B1] = ???

  override def iterator: Iterator[(List[Token], List[(Probability, Token)])] = ???

  override def -(key: List[Token]): Map[List[Token], List[(Probability, Token)]] = ???
}
