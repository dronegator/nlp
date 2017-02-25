package com.github.dronegator.nlp.main.chain

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector}
import breeze.numerics._
import breeze.optimize.DiffFunction
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.main._
import com.github.dronegator.nlp.main.chain.NNSampleChain.{Hidden, Network, Quality}

import scala.util.Random

/**
  * Created by cray on 12/15/16.
  */

object NNSampleChain {

  case class Quality()

  case class Network(tokenToKlassen: DenseMatrix[Double], klassenToReKlassen: DenseMatrix[Double], reKlassenToToken: DenseMatrix[Double], indexes: Seq[Int])
    extends NetworkBase

  case class Hidden(klassenI: DenseVector[Double],
                    klassenO: DenseVector[Double],
                    reKlassenI: DenseVector[Double],
                    reKlassenO: DenseVector[Double],
                    tokenI: SparseVector[Double],
                    tokenO: SparseVector[Double])
}

trait NNChain
  extends NN[I, O, Hidden, Network] {

  def winnerGetsAll: Boolean

  override def forward(network: Network, hidden: Hidden, input: I): O =
    input match {
      case (in1, in2) =>
        hidden.klassenI(0 until nKlassen) := network.tokenToKlassen(::, in1) * 1.0

        hidden.klassenI(nKlassen until nKlassen * 2) := network.tokenToKlassen(::, in2) * 1.0

        hidden.klassenO := hidden.klassenI.map(x => 1 / (1 + exp(-x)))

        for (i <- network.indexes) {
          hidden.klassenO(i) = 0.0
        }

        if (winnerGetsAll) {
          val (i1x, v1x) = hidden.klassenO(0 until nKlassen).iterator.maxBy(_._2)

          val (i2x, v2x) = hidden.klassenO(nKlassen until nKlassen * 2).iterator.maxBy(_._2)

          val (i1n, v1n) = hidden.klassenO(0 until nKlassen).iterator.minBy(_._2)

          val (i2n, v2n) = hidden.klassenO(nKlassen until nKlassen * 2).iterator.minBy(_._2)

          hidden.klassenO := 0.0

          hidden.klassenO(i1x) = v1n

          hidden.klassenO(i2x + nKlassen) = v2n

          hidden.klassenO(i1n) = v1n

          hidden.klassenO(i2n + nKlassen) = v2n
        }


        hidden.reKlassenI := network.klassenToReKlassen * hidden.klassenO

        hidden.reKlassenO := hidden.reKlassenI.map(x => 1 / (1 + exp(-x)))

        hidden.tokenI := network.reKlassenToToken * hidden.reKlassenO

        hidden.tokenO := hidden.tokenI.map(x => 1 / (1 + exp(-x)))
    }

  val nKlassen: Int

  val nToken: Int

  override def hiddenInit(): Hidden =
    Hidden(
      klassenI = DenseVector.zeros[Double](2 * nKlassen),
      klassenO = DenseVector.zeros[Double](2 * nKlassen),
      reKlassenI = DenseVector.zeros[Double](nKlassen),
      reKlassenO = DenseVector.zeros[Double](nKlassen),
      tokenI = SparseVector.zeros[Double](nToken),
      tokenO = SparseVector.zeros[Double](nToken)
    )
}

case class NNChainImpl(network: Network, nKlassen: Int, nToken: Int)
  extends NNChain {
  require(network.tokenToKlassen.rows == nKlassen)

  val winnerGetsAll = false

  def apply(input: I) =
    forward(network, input)
}

class NNSampleChain(val nKlassen: Int,
                    val nToken: Int,
                    dropout: Int,
                    val winnerGetsAll: Boolean,
                    val sampling: Iterable[(I, O)],
                    val rate: Int)
  extends NN[I, O, Hidden, Network]
    with DiffFunction[DenseVector[Double]]
    with NNSampleTrait[I, O, Network, Hidden, Quality]
    with NNQuality[O, Quality]
    with NNCalcSparseVector
    with NNChain {

  override def network(vector: DenseVector[Double]): Network =
    Network(
      tokenToKlassen = vector(0 until nToken * nKlassen).asDenseMatrix.reshape(nKlassen, nToken),
      klassenToReKlassen = vector(nToken * nKlassen until (nToken * nKlassen + 2 * nKlassen * nKlassen)).asDenseMatrix.reshape(nKlassen, 2 * nKlassen),
      reKlassenToToken = vector((nToken * nKlassen + 2 * nKlassen * nKlassen) until (nToken * nKlassen + 2 * nKlassen * nKlassen + nKlassen * nToken)).asDenseMatrix.reshape(nToken, nKlassen),
      (0 until nKlassen * 2)
        .filter(_ => Random.nextInt(100) < dropout)
    )

  override def size: Token = nKlassen * nToken + 2 * nKlassen * nKlassen + nKlassen * nToken

  override def backward(nn: Network, gradient: Network, hidden: Hidden, input: I, output: O, result: O): Unit =
    input match {
      case (in1, in2) =>

        val Network(gTokenToKlassen, klassenToReklassen, gReKlassen2Token, _) = gradient

        val backOutI = ((result - output) :* (result :* (-result + 1.0))).toDenseVector

        gReKlassen2Token :+= (backOutI * hidden.reKlassenO.t)

        val backReKlassenO = nn.reKlassenToToken.t * backOutI

        val backReKlassenI = backReKlassenO :* hidden.reKlassenO :* (-hidden.reKlassenO + 1.0)

        klassenToReklassen :+= backReKlassenI * hidden.klassenO.t

        val backKlassenO = nn.klassenToReKlassen.t * backReKlassenI

        val backKlassenI = backKlassenO :* hidden.klassenO :* (-hidden.klassenO + 1.0)

        gTokenToKlassen(::, in1) :+= (backKlassenI(0 until nKlassen) * 1.0)

        gTokenToKlassen(::, in2) :+= (backKlassenI(nKlassen until 2 * nKlassen) * 1.0)
    }


  override def quality(quality: Quality, result: O): Quality = {
    Quality()
  }

  override def quality: Quality =
    Quality()
}
