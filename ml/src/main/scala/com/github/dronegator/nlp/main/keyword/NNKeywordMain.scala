package com.github.dronegator.nlp.main.keyword

import java.io.File

import breeze.linalg.{DenseMatrix, DenseVector}
import breeze.optimize.DiffFunction
import com.github.dronegator.nlp.CaseClassToMap._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.main.keyword.NNSampleKeywordYesNo.Network
import com.github.dronegator.nlp.main.{Concurent, MainConfig, _}
import com.github.dronegator.nlp.trace._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyImpl}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import configs.syntax._

import scala.collection.JavaConverters._
import scala.util.Random

/**
  * Created by cray on 12/15/16.
  */

trait NNKeywordFunctionConfig {
  val nKlassen: Int
  val nToken: Option[Int]
  val dropout: Int
  val winnerGetsAll: Boolean
}

trait NetworkBase {
  val termToKlassen: DenseMatrix[Double]
  val klassenToOut: DenseMatrix[Double]
  val indexes: Seq[Int]
}

case class NNKeywordConfig(crossvalidationRatio: Int,
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
                           windowRate: Int)
  extends MLCfg
    with NNKeywordFunctionConfig


trait NNKeywordMain[N <: NetworkBase]
  extends App
    with MainTools
    with MainConfig[NNKeywordConfig]
    with NLPTAppMlTools[NNKeywordConfig, I, O, N]
    with Concurent
    with LazyLogging {

  lazy val cfg: NNKeywordConfig = {
    val cfg = ConfigFactory.parseMap(switches.asJava)
      .withFallback(config.getConfig("keyword"))
      .extract[NNKeywordConfig].value

    cfg.toMap.toList.sortBy(_._1).foreach {
      case (key, value) =>
        println(f"${key}%-32s = ${value}%-10s")
    }

    cfg
  }

  lazy val nTokenMax = vocabulary.wordMap.keys.max + 1
  lazy val nToken = cfg.nToken.getOrElse(vocabulary.wordMap.keys.max)

  lazy val vocabulary: Vocabulary = load(new File(fileIn)).time { t =>
    logger.info(s"Vocabulary has loaded in time=$t")
  } match {
    case vocabulary: Vocabulary =>
      vocabulary

    case vocabulary =>
      vocabulary: VocabularyImpl
  }

  lazy val (sense, senseCrossValidation) = vocabulary.sense.partition(_ => Random.nextInt(100) > cfg.doubleCrossvalidationRatio)

  lazy val (auxiliary, auxiliaryCrossValidation) = vocabulary.auxiliary.partition(_ => Random.nextInt(100) > cfg.doubleCrossvalidationRatio)

  private def proSampling(tokens: Set[Token]) =
    vocabulary.nGram3.keysIterator
      .filter {
        case ws@w1 :: w2 :: w3 :: _ =>
          ((tokens contains w2) || (tokens contains w2)) && ws.forall(_ <= nToken)
      }

  private def convert(sampling: Iterator[List[Token]]) =
    sampling
      .collect {
        case ws@w1 :: w2 :: w3 :: _ =>
          lazy val input = (w1, w3)

          val output = DenseVector[Double](1)

          if (vocabulary.sense contains w2)
            Some(input -> (output := 1.0))
          else if (vocabulary.auxiliary contains w2)
            Some(input -> (output := 0.0))
          else None
      }
      .collect {
        case Some(sample) => sample
      }
      .toIterable

  lazy val sampling: Iterable[((Token, Token), O)] =
    convert(proSampling(sense ++ auxiliary))

  lazy val samplingDoubleCross =
    convert(proSampling(senseCrossValidation ++ auxiliaryCrossValidation))

  override def printNetwork(network: N): Unit = {
    println("== Classes: ")

    for (n <- (0 until network.termToKlassen.rows)) {
      val vector = network.termToKlassen(n, ::).t

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

  def net(network: N): NN[(Token, Token), DenseVector[Double], _, N]

  override def calc(sampling: Iterable[((Token, Token), O)]): Unit = {
    val calulate = net(nn.network(network))

    val map = sampling
      .foldLeft(Map[Int, (Double, DenseVector[Double])]()) {
        case (map, ((t1, t3), _)) =>
          val vector = calulate.forward(nn.network(network), (t1, t3))

          vocabulary.map2ToMiddle(t1 :: t3 :: Nil)
            .foldLeft(map) {
              case (map, (weight, t2)) =>
                map + (map.get(t2) match {
                  case Some((accumulate, outcome)) =>
                    t2 -> (accumulate + 1.0, outcome + vector)
                  case None =>
                    t2 -> (1.0, vector)
                })
            }
      }
      .map {
        case (token, (weight, estimation)) =>
          (token, estimation :/ weight)
      }

    map.toSeq
      .sortBy(_._2)
      .foreach {
        case (token, estimation) =>
          println(
            f"""${
              vocabulary.wordMap.getOrElse(token, "***")
            }%-20s ${estimation}""")
      }

    val senseErr = vocabulary.sense.flatMap(map.get).reduce(_ + _) :/ vocabulary.sense.size.toDouble

    val auxiliaryErr = vocabulary.auxiliary.flatMap(map.get).reduce(_ + _) :/ vocabulary.auxiliary.size.toDouble

    println(s"senseErr = $senseErr")

    println(s"auxiliaryErr = $auxiliaryErr")
  }


}

object NNKeywordMainImpl
  extends NNKeywordMain[Network] {
  override def nn: NNSampleTrait[(Token, Token), O, Network, _, _] with DiffFunction[DenseVector[Double]] =
    new NNSampleKeywordYesNo(
      nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = cfg.dropout,
      winnerGetsAll = cfg.winnerGetsAll,
      sampling = sampling,
      rate = cfg.windowRate)

  def net(network: Network): NN[(Token, Token), DenseVector[Double], _, Network] =
    new NNKeywordYesNoImpl(network, cfg.nKlassen)

  try {
    report
  } finally {
    system.terminate()
    mat.shutdown()
  }

  override def nnCross: DiffFunction[DenseVector[Double]] =
    new NNSampleKeywordYesNo(nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingCross,
      rate = cfg.windowRate)

  override def nnDoubleCross: DiffFunction[DenseVector[Double]] =
    new NNSampleKeywordYesNo(nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingDoubleCross,
      rate = cfg.windowRate)

}

object NNKeywordMainWithConst
  extends NNKeywordMain[NNSampleKeywordYesNoWithConst.Network] {
  override def nn: NNSampleTrait[(Token, Token), O, NNSampleKeywordYesNoWithConst.Network, _, _] with DiffFunction[DenseVector[Double]] =
    new NNSampleKeywordYesNoWithConst(
      nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = cfg.dropout,
      winnerGetsAll = cfg.winnerGetsAll,
      sampling = sampling,
      rate = cfg.windowRate)

  def net(network: NNSampleKeywordYesNoWithConst.Network): NN[(Token, Token), DenseVector[Double], _, NNSampleKeywordYesNoWithConst.Network] =
    new NNKeywordYesNoImplWithConst(network, cfg.nKlassen)

  override def nnCross: DiffFunction[DenseVector[Double]] =
    new NNSampleKeywordYesNoWithConst(nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingCross,
      rate = cfg.windowRate)

  override def nnDoubleCross: DiffFunction[DenseVector[Double]] =
    new NNSampleKeywordYesNoWithConst(nKlassen = cfg.nKlassen,
      nToken = nTokenMax,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingDoubleCross,
      rate = cfg.windowRate)

  try {
    report
  } finally {
    system.terminate()
    mat.shutdown()
  }
}
