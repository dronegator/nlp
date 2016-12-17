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

  case class Network(termToKlassen: DenseMatrix[Double], klassenToOut: DenseMatrix[Double], indexes: Seq[Int])

  case class Hidden(klassenI: DenseVector[Double],
                    klassenO: DenseVector[Double],
                    outI: DenseVector[Double],
                    outO: DenseVector[Double])

}

trait NNKeyword
  extends NN[(Token, Token), DenseVector[Double], Hidden, Network] {

  def winnerGetsAll: Boolean

  override def forward(n: Network, hidden: Hidden, input: (Token, Token)): DenseVector[Double]

  override def hiddenInit(): Hidden
}

trait NNSampleKeyword
  extends NNKeyword
    with DiffFunction[DenseVector[Double]]
    with NNSampleTrait[(Token, Token), DenseVector[Double], Network, Hidden, Quality]
    with NNQuality[DenseVector[Double], Quality]
    with NNCalcDenseVector