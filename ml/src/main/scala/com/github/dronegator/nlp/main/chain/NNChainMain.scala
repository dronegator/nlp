package com.github.dronegator.nlp.main.chain

import java.io.File

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector, sum}
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
                         learn: Boolean,
                         insignificance: Double,
                         oppression: Double,
                         windowRate: Int)
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

  lazy val nToken = cfg.nToken.getOrElse(vocabulary.wordMap.keys.max + 1)

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
        case (t1 :: t2 :: _, tokens) if t1 < nToken && t2 < nToken =>
          val out = SparseVector(vocabulary.wordMap.keys.max + 1)(tokens.collect { case (x, y) if y < 100 => (y, x) }: _*)
          val summa = sum(out)
          if (summa < 0.01) None
          else {
            out :/= summa
            Some((t1, t2) -> out)
          }
      }
      .flatten

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

  def net(vector: DenseVector[Double], network: N): NN[I, O, _, N]

  override def calc(sampling: Iterable[(I, O)]): Unit = {
    val calculate = net(network, nn.network(network))

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

        println(f"($t1%03d, $t2%03d) $w1%10s $w2%10s -> $words")
        println(f"($t1%03d, $t2%03d) $w1%10s $w2%10s -> $outputWords")
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

  val nTokenMax = vocabulary.wordMap.keys.max + 1

  def net(vector: DenseVector[Double], network: Network): NN[I, O, _, Network] =
    new NNChainImpl(vector, network, cfg.nKlassen, nTokenMax)

  override def nn: NNSampleTrait[I, O, Network, _, _] with DiffFunction[DenseVector[Double]] =
    new NNSampleChain(
      nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = cfg.dropout,
      winnerGetsAll = cfg.winnerGetsAll,
      sampling = samplingLearn,
      cfg.windowRate)

  override def nnCross: DiffFunction[DenseVector[Double]] =
    new NNSampleChain(nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingCross,
      cfg.windowRate)

  override def nnDoubleCross: DiffFunction[DenseVector[Double]] =
    new NNSampleChain(nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingDoubleCross,
      cfg.windowRate)

  try {
    report
  } finally {
    system.terminate()
    mat.shutdown()
  }


}

object NNChainMainWithConstImpl
  extends NNChainMain[NNSampleChainWithConst.Network] {

  val nTokenMax = vocabulary.wordMap.keys.max + 1

  def net(vector: DenseVector[Double], network: NNSampleChainWithConst.Network): NN[I, O, _, NNSampleChainWithConst.Network] =
    new NNChainWithConstImpl(vector, network, cfg.nKlassen, nTokenMax)

  override def nn: NNSampleTrait[I, O, NNSampleChainWithConst.Network, _, _] with DiffFunction[DenseVector[Double]] =
    new NNSampleChainWithConst(
      nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = cfg.dropout,
      winnerGetsAll = cfg.winnerGetsAll,
      sampling = samplingLearn,
      insignificance = cfg.insignificance,
      oppression = cfg.oppression,
      rate = cfg.windowRate)

  override def nnCross: DiffFunction[DenseVector[Double]] =
    new NNSampleChainWithConst(nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingCross,
      insignificance = cfg.insignificance,
      oppression = cfg.oppression,
      rate = cfg.windowRate)

  override def nnDoubleCross: DiffFunction[DenseVector[Double]] =
    new NNSampleChainWithConst(nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingDoubleCross,
      insignificance = cfg.insignificance,
      oppression = cfg.oppression,
      rate = cfg.windowRate)


  try {
    report
  } finally {
    system.terminate()
    mat.shutdown()
  }
}

