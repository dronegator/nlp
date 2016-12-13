package com.github.dronegator.nlp.main.keyword

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.optimize.DiffFunction
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.main._
import com.github.dronegator.nlp.main.keyword.NNSampleKeyword.{Hidden, Network, Quality}

/**
  * Created by cray on 12/13/16.
  */
object NNSampleKeyword {

  case class Quality(yes: Int, no: Int)

  case class Network(termToKlassen: DenseMatrix[Double], klassenToOut: DenseMatrix[Double])

  case class Hidden()

}

class NNKeyword
  extends NN[(Token, Token), DenseVector[Double], Hidden] {
  override def forward(hidden: Hidden, input: (Token, Token)): DenseVector[Double] = ???

  override def hiddenInit(): Hidden = ???
}

case class NNKeywordImpl(network: Network) extends NNKeyword {
  def apply(input: (Token, Token)) =
    forward(input)
}

class NNSampleKeyword
  extends NNKeyword
    with DiffFunction[DenseVector[Double]]
    with NNSampleTrait[(Token, Token), DenseVector[Double], Network, Hidden, Quality]
    with NNQuality[DenseVector[Double], Quality]
    with NNCalcDenseVector {
  override def network(vector: DenseVector[Double]): Network = ???

  override def size: Token = ???

  override def sampling: Iterable[((Token, Token), DenseVector[Double])] = ???

  override def backward(hidden: Hidden, input: (Token, Token), output: DenseVector[Double], result: DenseVector[Double]): DenseVector[Double] = ???

  override def quality(quality: Quality, result: DenseVector[Double]): Quality = ???

  override def quality: Quality = ???
}
