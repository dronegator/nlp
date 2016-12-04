package com.github.dronegator.nlp.main.NNExample

import java.io.File

import breeze.linalg.{DenseVector, SparseVector}
import breeze.optimize.{DiffFunction, LBFGS}
import breeze.util.Implicits._
import com.github.dronegator.nlp.main.{Concurent, MainConfig, MainTools}
import com.github.dronegator.nlp.trace._
import com.github.dronegator.nlp.utils.Match._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyImpl}
import com.typesafe.scalalogging.LazyLogging
import configs.syntax._

object TeachKeywordSelectorMain
  extends App
    with MainTools
    with MainConfig[NLPTAppForNNConfig]
    with Concurent
    with LazyLogging
    with NLPTAppForNN {

  val fileIn :: OptFile(hints) = args.toList

  lazy val cfg = config.get[NLPTAppForNNConfig]("nn").value

  lazy val vocabulary: Vocabulary = load(new File(fileIn)).time { t =>
    logger.info(s"Vocabulary has loaded in time=$t")
  } match {
    case vocabulary: Vocabulary =>
      vocabulary

    case vocabulary =>
      vocabulary: VocabularyImpl
  }

  val nToken = vocabulary.wordMap.keys.max: Int

  def samples = vocabulary.nGram3.keysIterator
    .map {
      case ws@w1 :: w2 :: w3 :: _ if ws.forall(_ <= nToken) =>
        lazy val input = SparseVector(nToken * 2)(w1 -> 1.0, w3 + nToken -> 1.0)

        if (vocabulary.sense contains w2)
          Some(input -> SparseVector(1)(0 -> 1.0))
        else if (vocabulary.auxiliary contains w2)
          Some(input -> SparseVector(1)(0 -> 0.0))
        else None
    }
    .collect {
      case Some(sample) => sample
    }

  val lbfgs = new LBFGS[DenseVector[Double]](maxIter = cfg.maxIter) //tolerance = 1E-2)

  println(s"size = ${cfg.nSample getOrElse samples.length}")
  println(s"nToken = ${nToken}")
  println(s"nKlassen = ${cfg.nKlassen}")
  println(s"regularization = ${cfg.regularization}")

  val nn = new NN(cfg.nKlassen, nToken, cfg.nSample.map(samples.take(_)).getOrElse(samples))

  //  val network = lbfgs.minimize(nn, DenseVector.rand(2*10+10*2))

  val network = lbfgs.iterations(DiffFunction.withL2Regularization(nn, cfg.regularization), nn.initial :/ 1.0)
    .map {
      case x =>
        println(x.value)
        x.x
    }
    .last

  println("stop")

  val (termToKlassen, _) = nn.network(network)

  for (i <- (0 until cfg.nKlassen)) {
    val vector = termToKlassen(i, ::).t

    println(s"====== $i")
    val tokens = vector.toScalaVector()
      .zipWithIndex
      .sortBy(-_._1)
      .map(x => x._2 -> x._1)

    //(tokens.take(20) ++ tokens.takeRight(20))
    tokens.foreach {
      case (token, weight) =>
        println(f"${vocabulary.wordMap(token)}%10s $weight")

    }


  }

  system.shutdown()
}
