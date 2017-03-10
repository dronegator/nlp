package com.github.dronegator.nlp.main.chain

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector, sum}
import breeze.numerics._
import breeze.optimize.DiffFunction
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.main._
import com.github.dronegator.nlp.main.chain.NNSampleChainWithConst.{Hidden, Network, Quality}

import scala.util.Random

/**
  * Created by cray on 12/15/16.
  */

object NNSampleChainWithConst {

  case class Quality()

  case class Network(tokenToKlassen: DenseMatrix[Double],
                     klassenToReKlassen: DenseMatrix[Double],
                     reKlassenToToken: DenseMatrix[Double],
                     constToKlassen: DenseVector[Double],
                     constToReKlassen: DenseVector[Double],
                     constToToken: DenseVector[Double],
                     indexes: Seq[Int])
    extends NetworkBase

  case class Hidden(klassenI: DenseVector[Double],
                    klassenO: DenseVector[Double],
                    reKlassenI: DenseVector[Double],
                    reKlassenO: DenseVector[Double],
                    tokenI: SparseVector[Double],
                    tokenO: SparseVector[Double])

}

trait NNChainWithConst
  extends NN[I, O, Hidden, Network] {

  def winnerGetsAll: Boolean

  override def forward(network: Network, hidden: Hidden, input: I): O =
    input match {
      case (in1, in2) =>
        hidden.klassenI(0 until nKlassen) := network.tokenToKlassen(::, in1) * 1.0

        hidden.klassenI(0 until nKlassen) += network.constToKlassen(0 until nKlassen) * 1.0

        hidden.klassenI(nKlassen until nKlassen * 2) := network.tokenToKlassen(::, in2) * 1.0

        hidden.klassenI(nKlassen until nKlassen * 2) += network.constToKlassen(0 until nKlassen) * 1.0

        hidden.klassenO := hidden.klassenI.map(x => 1 / (1 + exp(-x)))

        for (i <- network.indexes) {
          hidden.klassenO(i) = 0.0
        }

        hidden.reKlassenI := network.klassenToReKlassen * hidden.klassenO

        hidden.reKlassenI += network.constToReKlassen * 1.0

        hidden.reKlassenO := hidden.reKlassenI.map(x => 1 / (1 + exp(-x)))

        hidden.tokenI := network.reKlassenToToken * hidden.reKlassenO

        hidden.tokenI += network.constToToken * 1.0

        //hidden.tokenO := hidden.tokenI.map(x => 1 / (1 + exp(-x)))
        // Softmax

        val denominator = sum(hidden.tokenI.map(exp(_)))

        if (denominator > 1e10) println(denominator)

        hidden.tokenO := hidden.tokenI.map(exp(_) / denominator)
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

case class NNChainWithConstImpl(vector: DenseVector[Double], network: Network, nKlassen: Int, nToken: Int)
  extends NNChainWithConst
    with NNForw[I, O, Hidden, Network] {
  require(network.tokenToKlassen.rows == nKlassen)

  val winnerGetsAll = false
}

class NNSampleChainWithConst(val nKlassen: Int,
                             val nToken: Int,
                             dropout: Int,
                             val winnerGetsAll: Boolean,
                             insignificance: Double,
                             oppression: Double,
                             val sampling: Iterable[(I, O)],
                             val rate: Int)
  extends NN[I, O, Hidden, Network]
    with DiffFunction[DenseVector[Double]]
    with NNSampleTrait[I, O, Network, Hidden, Quality]
    with NNQuality[O, Quality]
    with NNCalcSparseVector
    with NNChainWithConst {

  val EndOfTokenToKlassen = nToken * nKlassen
  val EndOfKlassenToReKlassen = EndOfTokenToKlassen + 2 * nKlassen * nKlassen
  val EndOfReKlassenToToken = EndOfKlassenToReKlassen + nKlassen * nToken
  val EndOfConstToKlassen = EndOfReKlassenToToken + nKlassen
  val EndOfConstToReclassen = EndOfConstToKlassen + nKlassen
  val EndOfConstToToken = EndOfConstToReclassen + nToken

  override def net(vector: DenseVector[Double]): NNChainWithConstImpl =
    NNChainWithConstImpl(vector, network(vector), nKlassen, nToken)

  override def network(vector: DenseVector[Double]): Network = {
    Network(
      tokenToKlassen =
        vector(0 until EndOfTokenToKlassen).asDenseMatrix.reshape(nKlassen, nToken),
      klassenToReKlassen =
        vector(EndOfTokenToKlassen until EndOfKlassenToReKlassen).asDenseMatrix.reshape(nKlassen, 2 * nKlassen),
      reKlassenToToken =
        vector(EndOfKlassenToReKlassen until EndOfReKlassenToToken).asDenseMatrix.reshape(nToken, nKlassen),
      constToKlassen = vector(EndOfReKlassenToToken until EndOfConstToKlassen),
      constToReKlassen = vector(EndOfConstToKlassen until EndOfConstToReclassen),
      constToToken = vector(EndOfConstToReclassen until EndOfConstToToken),
      indexes = (0 until nKlassen * 2)
        .filter(_ => Random.nextInt(100) < dropout)
    )
  }

  override def size: Token = EndOfConstToToken

  //  override def error(output: SparseVector[Double], result: SparseVector[Double]): Double = {
  //    val v = (result - output)
  //    output.iterator
  //      .map {
  //        case (n, x) =>
  //          if (x < insignificance)
  //            v(n) * oppression
  //          else
  //            v(n) * v(n)
  //      }
  //      .sum / v.iterableSize
  //  }

  override def backward(nn: Network, gradient: Network, hidden: Hidden, input: I, output: O, result: O): Unit =
    input match {
      case (in1, in2) =>

        val Network(gTokenToKlassen, klassenToReklassen, gReKlassen2Token, constToKlassen, constToReKlassen, constToToken, _) =
          gradient

        val backerr = result - output

        //        output.iterator.foreach {
        //          case (n, x) =>
        //            if (x < insignificance)
        //              backerr.update(n, oppression)
        //        }

        //backerr :+= oppression

        //backerr :/= backerr.iterableSize.toDouble

        //val backOutI = (backerr :* (result :* (-result + 1.0))).toDenseVector
        //Try Softmax function


        val backOutI = DenseVector.fill(nToken)(0.0)
        def kronekerDelta(i: Int, j: Int) =
          if (i == j) 1.0 else 0.0

        (0 until nToken)
          .foreach { k =>
            val backOutIk = DenseVector.fill(nToken)(0.0)

            (0 until nToken)
              .foreach { j =>
                backOutIk.update(j, result(j) * (kronekerDelta(j, k) - result(k)))
              }

            backOutIk :+= backerr

            backOutI :+= backOutIk

          }

        //println(backOutI)

        gReKlassen2Token :+= (backOutI * hidden.reKlassenO.t)

        constToToken :+= backOutI

        val backReKlassenO = nn.reKlassenToToken.t * backOutI

        val backReKlassenI = backReKlassenO :* hidden.reKlassenO :* (-hidden.reKlassenO + 1.0)

        klassenToReklassen :+= backReKlassenI * hidden.klassenO.t

        constToReKlassen := backReKlassenI

        val backKlassenO = nn.klassenToReKlassen.t * backReKlassenI

        val backKlassenI = backKlassenO :* hidden.klassenO :* (-hidden.klassenO + 1.0)

        constToKlassen :+= backKlassenI(0 until nKlassen)

        constToKlassen :+= backKlassenI(nKlassen until 2 * nKlassen)

        gTokenToKlassen(::, in1) :+= (backKlassenI(0 until nKlassen) * 1.0)

        gTokenToKlassen(::, in2) :+= (backKlassenI(nKlassen until 2 * nKlassen) * 1.0)
    }


  override def quality(quality: Quality, result: O): Quality = {
    Quality()
  }

  override def quality: Quality =
    Quality()
}
