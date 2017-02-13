package com.github.dronegator.nlp.main.chain

import java.io.File

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector}
import breeze.optimize.DiffFunction
import com.github.dronegator.nlp.CaseClassToMap._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.main.chain.NNSampleChain.Network
import com.github.dronegator.nlp.main.{Concurent, MainConfig, _}
import com.github.dronegator.nlp.trace._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyImpl}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import configs.syntax._

import scala.collection.JavaConverters._

/**
  * Created by cray on 12/15/16.
  */

trait NNChainFunctionConfig {
  val nKlassen: Int
  val nToken: Option[Int]
  val dropout: Int
  val winnerGetsAll: Boolean
}

trait NetworkBase {
  val tokenToKlassen: DenseMatrix[Double]
  val reKlassenToToken: DenseMatrix[Double]
  val indexes: Seq[Int]
}

case class NNChainConfig(crossvalidationRatio: Int,
                         doubleCrossvalidationRatio: Int,
                         algorithm: Algorithm,
                         regularization: Double,
                         range: Double,
                         maxIter: Int,
                         rfo: Double,
                         tolerance: Double,
                         memoryLimit: Int,
                         crossvalidationFrequency: Int,
                         nKlassen: Int,
                         nToken: Option[Int],
                         dropout: Int,
                         winnerGetsAll: Boolean,
                         lambda: Double,
                         delta: Double,
                         eta: Double,
                         stepSize: Double,
                         minImprovementWindow: Token,
                         learn: Boolean)
  extends MLCfg
    with NNChainFunctionConfig


trait NNChainMain[N <: NetworkBase]
  extends App
    with MainTools
    with MainConfig[NNChainConfig]
    with NLPTAppMlTools[NNChainConfig, I, O, N]
    with Concurent
    with LazyLogging {

  lazy val cfg: NNChainConfig = {
    val cfg = ConfigFactory.parseMap(switches.asJava)
      .withFallback(config.getConfig("chain"))
      .extract[NNChainConfig].value

    cfg.toMap.toList.sortBy(_._1).foreach {
      case (key, value) =>
        println(f"${key}%-32s = ${value}%-10s")
    }

    cfg
  }

  lazy val nToken = cfg.nToken.getOrElse(vocabulary.wordMap.keys.max)

  lazy val vocabulary: Vocabulary =
    if (fileIn == "Test") {
      new TestVocabulary: VocabularyImpl
    } else {
      load(new File(fileIn)).time { t =>
        logger.info(s"Vocabulary has loaded in time=$t")
      } match {
        case vocabulary: Vocabulary =>
          vocabulary

        case vocabulary =>
          vocabulary: VocabularyImpl
      }
    }


  lazy val sampling: Iterable[(I, O)] =
    vocabulary.map2ToNext
      .collect {
        case (t1 :: t2 :: _, tokens) =>
          (t1, t2) -> SparseVector(nToken)(tokens.map { case (x, y) => (y, x) }: _*)
      }

  lazy val samplingDoubleCross =
    Iterable.empty[(I, O)]

  override def printNetwork(network: N): Unit = {
    println("== Classes: ")

    for (n <- (0 until network.tokenToKlassen.rows)) {
      val vector = network.tokenToKlassen(n, ::).t

      println(s"====== $n ==")
      val tokens = vector.toScalaVector()
        .zipWithIndex
        .sortBy(-_._1)
        .map(x => x._2 -> x._1)

      tokens.foreach {
        case (token, weight) =>
          println(f"${
            vocabulary.wordMap(token)
          }%10s $weight")

      }
    }

  }

  implicit val orderingDenseVectorDouble = Ordering.fromLessThan[DenseVector[Double]] {
    (x, y) =>
      (x :> y).forall(x => x)
  }

  def net(network: N): NN[I, O, _, N]

  override def calc(sampling: Iterable[(I, O)]): Unit = {
    val calculate = net(nn.network(network))

    sampling.foreach {
      case ((t1, t2), output) =>
        val vector = calculate.forward(nn.network(network), (t1, t2))
        val words = vector.activeIterator.toList.sortBy(-_._2).take(20).takeWhile(_._2 > 0.001)
          .flatMap {
            case (x, y) =>
              vocabulary.wordMap.get(x).map(x => (x, f"${y}%5.3f" /**/ ))
          }
          .mkString(" ")

        val w1 = vocabulary.wordMap.get(t1).getOrElse("***")
        val w2 = vocabulary.wordMap.get(t2).getOrElse("***")

        val outputWords = output.activeIterator.toList.sortBy(-_._2).take(20).takeWhile(_._2 > 0.001)
          .flatMap {
            case (x, y) =>
              vocabulary.wordMap.get(x).map(x => (x, f"${y}%5.3f" /**/ ))
          }
          .mkString(" ")

        println(f"$w1%10s $w2%10s -> $words")
        println(f"$w1%10s $w2%10s -> $outputWords")
    }

    // TODO: We have to find a way to represent quality of the service
    //    val calulate = net(nn.network(network))
    //
    //    val map = sampling
    //      .foldLeft(Map[Int, (Double, DenseVector[Double])]()) {
    //        case (map, ((t1, t3), _)) =>
    //          val vector = calulate.forward(nn.network(network), (t1, t3))
    //
    //          vocabulary.map2ToMiddle(t1 :: t3 :: Nil)
    //            .foldLeft(map) {
    //              case (map, (weight, t2)) =>
    //                map + (map.get(t2) match {
    //                  case Some((accumulate, outcome)) =>
    //                    t2 -> (accumulate + 1.0, outcome + vector)
    //                  case None =>
    //                    t2 -> (1.0, vector)
    //                })
    //            }
    //      }
    //      .map {
    //        case (token, (weight, estimation)) =>
    //          (token, estimation :/ weight)
    //      }
    //
    //    map.toSeq
    //      .sortBy(_._2)
    //      .foreach {
    //        case (token, estimation) =>
    //          println(
    //            f"""${
    //              vocabulary.wordMap.getOrElse(token, "***")
    //            }%-20s ${estimation}""")
    //      }
    //
    //    val senseErr = vocabulary.sense.flatMap(map.get).reduce(_ + _) :/ vocabulary.sense.size.toDouble
    //
    //    val auxiliaryErr = vocabulary.auxiliary.flatMap(map.get).reduce(_ + _) :/ vocabulary.auxiliary.size.toDouble
    //
    //    println(s"senseErr = $senseErr")
    //
    //    println(s"auxiliaryErr = $auxiliaryErr")
  }


}

object NNChainMainImpl
  extends NNChainMain[Network] {

  def net(network: Network): NN[I, O, _, Network] =
    new NNChainImpl(network, cfg.nKlassen, nToken)

  override def nn: NNSampleTrait[I, O, Network, _, _] with DiffFunction[DenseVector[Double]] =
    new NNSampleChain(
      nKlassen = cfg.nKlassen,
      nToken = nToken,
      dropout = cfg.dropout,
      winnerGetsAll = cfg.winnerGetsAll,
      sampling = samplingLearn)

  override def nnCross: DiffFunction[DenseVector[Double]] =
    new NNSampleChain(nKlassen = cfg.nKlassen,
      nToken = nToken,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingCross)

  override def nnDoubleCross: DiffFunction[DenseVector[Double]] =
    new NNSampleChain(nKlassen = cfg.nKlassen,
      nToken = nToken,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingDoubleCross)

  try {
    report
  } finally {
    system.terminate()
    mat.shutdown()
  }


}

object NNChainMainWithConstImpl
  extends NNChainMain[NNSampleChainWithConst.Network] {

  def net(network: NNSampleChainWithConst.Network): NN[I, O, _, NNSampleChainWithConst.Network] =
    new NNChainWithConstImpl(network, cfg.nKlassen, nToken)

  override def nn: NNSampleTrait[I, O, NNSampleChainWithConst.Network, _, _] with DiffFunction[DenseVector[Double]] =
    new NNSampleChainWithConst(
      nKlassen = cfg.nKlassen,
      nToken = nToken,
      dropout = cfg.dropout,
      winnerGetsAll = cfg.winnerGetsAll,
      sampling = samplingLearn)

  override def nnCross: DiffFunction[DenseVector[Double]] =
    new NNSampleChainWithConst(nKlassen = cfg.nKlassen,
      nToken = nToken,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingCross)

  override def nnDoubleCross: DiffFunction[DenseVector[Double]] =
    new NNSampleChainWithConst(nKlassen = cfg.nKlassen,
      nToken = nToken,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingDoubleCross)

  try {
    report
  } finally {
    system.terminate()
    mat.shutdown()
  }
}

