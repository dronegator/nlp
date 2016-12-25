//package com.github.dronegator.nlp.main.chain
//
//import breeze.linalg.{DenseMatrix, DenseVector}
//import breeze.numerics._
//import breeze.optimize.DiffFunction
//import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
//import com.github.dronegator.nlp.main.chain.NNSampleChainYesNoWithConst.{Hidden, Network, Quality}
//import com.github.dronegator.nlp.main.{NN, NNCalcDenseVector, NNQuality, NNSampleTrait}
//
//import scala.util.Random
//
///**
//  * Created by cray on 12/15/16.
//  */
//
//object NNSampleChainYesNoWithConst {
//
//  case class Quality(yes: Int, no: Int)
//
//  case class Network(constToKlassen: DenseVector[Double], tokenToKlassen: DenseMatrix[Double], reKlassenToToken: DenseMatrix[Double], indexes: Seq[Int])
//    extends NetworkBase
//
//  case class Hidden(klassenI: DenseVector[Double],
//                    klassenO: DenseVector[Double],
//                    outI: DenseVector[Double],
//                    outO: DenseVector[Double])
//
//}
//
//trait NNChainYesNoWithConst
//  extends NN[(Token, Token), DenseVector[Double], Hidden, Network] {
//
//  def winnerGetsAll: Boolean
//
//  override def forward(network: Network, hidden: Hidden, input: (Token, Token)): O =
//    input match {
//      case (in1, in2) =>
//        hidden.klassenI(0 until nKlassen) := network.tokenToKlassen(::, in1) * 1.0
//
//        hidden.klassenI(0 until nKlassen) :+= network.constToKlassen * 1.0
//
//        hidden.klassenI(nKlassen until nKlassen * 2) := network.tokenToKlassen(::, in2) * 1.0
//
//        hidden.klassenI(nKlassen until nKlassen * 2) :+= network.constToKlassen * 1.0
//
//        hidden.klassenO := hidden.klassenI.map(x => 1 / (1 + exp(-x)))
//
//        for (i <- network.indexes) {
//          hidden.klassenO(i) = 0.0
//        }
//
//        if (winnerGetsAll) {
//          val (i1x, v1x) = hidden.klassenO(0 until nKlassen).iterator.maxBy(_._2)
//
//          val (i2x, v2x) = hidden.klassenO(nKlassen until nKlassen * 2).iterator.maxBy(_._2)
//
//          val (i1n, v1n) = hidden.klassenO(0 until nKlassen).iterator.minBy(_._2)
//
//          val (i2n, v2n) = hidden.klassenO(nKlassen until nKlassen * 2).iterator.minBy(_._2)
//
//          hidden.klassenO := 0.0
//
//          hidden.klassenO(i1x) = v1n
//
//          hidden.klassenO(i2x + nKlassen) = v2n
//
//          hidden.klassenO(i1n) = v1n
//
//          hidden.klassenO(i2n + nKlassen) = v2n
//        }
//
//        hidden.outI := network.reKlassenToToken * hidden.klassenO
//
//        hidden.outO := hidden.outI.map(x => 1 / (1 + exp(-x)))
//    }
//
//  val nKlassen: Int
//
//  override def hiddenInit(): Hidden =
//    Hidden(
//      klassenI = DenseVector.zeros[Double](2 * nKlassen),
//      klassenO = DenseVector.zeros[Double](2 * nKlassen),
//      outI = DenseVector.zeros[Double](1),
//      outO = DenseVector.zeros[Double](1)
//    )
//}
//
//case class NNChainYesNoImplWithConst(network: Network, nKlassen: Int)
//  extends NNChainYesNoWithConst {
//  require(network.tokenToKlassen.rows == nKlassen)
//
//  val winnerGetsAll = false
//
//  def apply(input: (Token, Token)) =
//    forward(network, input)
//}
//
//class NNSampleChainYesNoWithConst(val nKlassen: Int,
//                                  nToken: Int,
//                                  dropout: Int,
//                                  val winnerGetsAll: Boolean,
//                                  val sampling: Iterable[((Token, Token), DenseVector[Double])])
//  extends NN[(Token, Token), DenseVector[Double], Hidden, Network]
//    with DiffFunction[DenseVector[Double]]
//    with NNSampleTrait[(Token, Token), DenseVector[Double], Network, Hidden, Quality]
//    with NNQuality[DenseVector[Double], Quality]
//    with NNCalcDenseVector
//    with NNChainYesNoWithConst {
//
//  override def network(vector: DenseVector[Double]): Network =
//    Network(
//      constToKlassen = vector(nToken * nKlassen + nKlassen * 2 until nToken * nKlassen + nKlassen * 2 + nKlassen),
//      tokenToKlassen = vector(0 until nToken * nKlassen).asDenseMatrix.reshape(nKlassen, nToken),
//      reKlassenToToken = vector(nToken * nKlassen until (nToken * nKlassen + nKlassen * 2)).asDenseMatrix.reshape(1, nKlassen * 2),
//      (0 until nKlassen * 2)
//        .filter(_ => Random.nextInt(100) < dropout)
//    )
//
//  override def size: Token = nKlassen * nToken + nKlassen * 2 + nKlassen
//
//  override def backward(nn: Network, gradient: Network, hidden: Hidden, input: (Token, Token), output: DenseVector[Double], result: DenseVector[Double]): Unit =
//
//    input match {
//      case (in1, in2) =>
//
//        val Network(gConstToKlassen, gTermToKlassen, gKlassen2Out, _) = gradient
//
//        val backOutI = (result - output) :* (result :* (-result + 1.0))
//
//        gKlassen2Out :+= (backOutI * hidden.klassenO.t)
//
//        val backKlassenO = nn.reKlassenToToken.t * backOutI
//
//        val backKlassenI = backKlassenO :* hidden.klassenO :* (-hidden.klassenO + 1.0)
//
//        gConstToKlassen :+= (backKlassenI(0 until nKlassen) * 1.0)
//
//        gConstToKlassen :+= (backKlassenI(nKlassen until 2 * nKlassen) * 1.0)
//
//        gTermToKlassen(::, in1) :+= (backKlassenI(0 until nKlassen) * 1.0)
//
//        gTermToKlassen(::, in2) :+= (backKlassenI(nKlassen until 2 * nKlassen) * 1.0)
//    }
//
//
//  override def quality(quality: Quality, result: DenseVector[Double]): Quality = {
//    Quality(
//      yes = if (result.exists(_ > 0.9)) quality.yes + 1 else quality.yes,
//      no = if (result.exists(_ < 0.1)) quality.no + 1 else quality.no)
//  }
//
//  override def quality: Quality =
//    Quality(0, 0)
//
//}
