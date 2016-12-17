package com.github.dronegator.nlp.main.keyword

import java.io.File

import breeze.linalg.DenseVector
import breeze.optimize.DiffFunction
import com.github.dronegator.nlp.CaseClassToMap._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.main.keyword.NNSampleKeyword.Network
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
                           winnerGetsAll: Boolean)
  extends MLCfg
    with NNKeywordFunctionConfig


object NNKeywordMain
  extends App
    with MainTools
    with MainConfig[NNKeywordConfig]
    with NLPTAppMlTools[NNKeywordConfig, I, O, Network]
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

  lazy val sampling: Iterable[((Token, Token), O)] =
    vocabulary.nGram3.keysIterator
      .collect {
        case ws@w1 :: w2 :: w3 :: _ if ws.forall(_ <= nToken) =>
          lazy val input = (w1, w3)

          val output = DenseVector[Double](1)

          if (sense contains w2)
            Some(input -> (output := 1.0))
          else if (auxiliary contains w2)
            Some(input -> (output := 0.0))
          else None
      }
      .collect {
        case Some(sample) => sample
      }
      .toIterable

  lazy val samplingDoubleCross =
    vocabulary.nGram3.keysIterator
      .map {
        case ws@w1 :: w2 :: w3 :: _ if ws.forall(_ <= nToken) =>
          lazy val input = (w1, w3)

          val output = DenseVector[Double](1)

          if (senseCrossValidation contains w2)
            Some(input -> (output := 1.0))
          else if (auxiliaryCrossValidation contains w2)
            Some(input -> (output := 0.0))
          else None
      }
      .collect {
        case Some(sample) => sample
      }
      .toIterable


  override def nn: NNSampleTrait[(Token, Token), O, Network, _, _] with DiffFunction[DenseVector[Double]] =
    new NNSampleKeywordYesNo(nKlassen = cfg.nKlassen,
      nToken = nToken,
      dropout = cfg.dropout,
      winnerGetsAll = cfg.winnerGetsAll,
      sampling = sampling)

  override def nnCross: DiffFunction[DenseVector[Double]] =
    new NNSampleKeywordYesNo(nKlassen = cfg.nKlassen,
      nToken = nToken,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingCross)

  override def nnDoubleCross: DiffFunction[DenseVector[Double]] =
    new NNSampleKeywordYesNo(nKlassen = cfg.nKlassen,
      nToken = nToken,
      dropout = 0,
      winnerGetsAll = false,
      sampling = samplingDoubleCross)

  override def printNetwork(network: Network): Unit = {
    println("== Classes: ")

    for (n <- (0 until network.termToKlassen.rows)) {
      val vector = network.termToKlassen(n, ::).t

      println(s"====== $n ==")
      val tokens = vector.toScalaVector()
        .zipWithIndex
        .sortBy(-_._1)
        .map(x => x._2 -> x._1)

      //(tokens.take(20) ++ tokens.takeRight(20))
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

  override def calc(sampling: Iterable[((Token, Token), O)]): Unit = {
    val calulate = new NNKeywordYesNoImpl(nn.network(network), cfg.nKlassen)

    sampling
      .foldLeft(Map[Int, (Double, DenseVector[Double])]()) {
        case (map, ((t1, t3), _)) =>
          val vector = calulate.apply((t1, t3))

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
      .toSeq
      .map {
        case (token, (weight, estimation)) =>
          (token, estimation :/ weight)
      }
      .sortBy(_._2)
      .foreach {
        case (token, estimation) =>
          println(
            f"""${
              vocabulary.wordMap.getOrElse(token, "***")
            }%-20s ${estimation}""")
      }

  }

  try {
    report
  } finally {
    system.terminate()
    mat.shutdown()
  }

}
