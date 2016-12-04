package com.github.dronegator.nlp.main.NNExample

import java.io.File

import breeze.linalg.{DenseVector, SparseVector}
import breeze.numerics._
import breeze.optimize.{DiffFunction, LBFGS}
import breeze.util.Implicits._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
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

  val lbfgs = new LBFGS[DenseVector[Double]](maxIter = cfg.maxIter, m = cfg.memoryLimit, tolerance = cfg.tolerance)

  println(
    s"""
      size = ${cfg.nSample getOrElse samples.length}
      nToken = ${nToken}
      nKlassen = ${cfg.nKlassen}
      regularization = ${cfg.regularization}
      dropout = ${cfg.dropout}
      maxIter = ${cfg.maxIter}
      range = ${cfg.range}
      tolerance = ${cfg.tolerance}
      memoryLimit = ${cfg.memoryLimit}
    """)

  val nn = new NN(cfg.nKlassen, nToken, cfg.dropout, cfg.nSample.map(samples.take(_)).getOrElse(samples))

  //  val network = lbfgs.minimize(nn, DenseVector.rand(2*10+10*2))

  val network = lbfgs.iterations(
    if (cfg.regularization < 0.000001) nn else DiffFunction.withL2Regularization(nn, cfg.regularization),
    ((nn.initial :* 2.0) :- 1.0) :* cfg.range)
    .map {
      case x =>
        println(x.value)
        x.x
    }
    .last

  println("stop")

  val (termToKlassen, klassenToOut) = nn.network(network)

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

  println(s"====== ==")
  implicit val orderingDenseVectorDouble = Ordering.fromLessThan[DenseVector[Double]] { (x, y) =>
    (x :< y).forall(x => x)
  }

  def calc(samples: Iterable[(List[Token], List[(Double, Token)])]) =
    samples.collect {
      case (w1 :: w3 :: _, ws) =>
        val input = SparseVector(nToken * 2)(w1 -> 1.0, w3 + nToken -> 1.0)

        val klassenI = DenseVector.zeros[Double](2 * cfg.nKlassen)

        klassenI(0 until cfg.nKlassen) := termToKlassen * input(0 until nToken)

        klassenI(cfg.nKlassen until cfg.nKlassen * 2) := termToKlassen * input(nToken until nToken * 2)

        val klassenO = klassenI.map(x => 1 / (1 + exp(-x)))

        val outI = DenseVector.zeros[Double](1)

        outI := klassenToOut * klassenO

        val outO = outI.map(x => 1 / (1 + exp(-x)))

        (ws, outO)
    }
      .foldLeft(Map[Token, (Int, DenseVector[Double])]()) {
        case (map, (ws, outO)) =>
          ws.foldLeft(map) {
            case (map, (_, token)) =>
              val v = map.get(token) match {
                case Some((n, v)) =>
                  (n + 1, v + outO)
                case None =>
                  (1, outO)
              }

              map + (token -> v)
          }
      }
      .map {
        case (token, (n, outO)) =>
          (token, outO :/ n.toDouble)
      }
      .toList
      .sortBy {
        case (token, outO) =>
          outO
      }

  println(" ====== From samples:")
  val sampledTokens = calc(vocabulary.map2ToMiddle.collect {
    case record@(w1 :: w3 :: _, ws) if ws.map(_._2).exists(x => vocabulary.sense(x) || vocabulary.auxiliary(x)) =>
      record
  })
    .foldLeft(Set[Token]()) {
      case (set, (token, outO)) =>
        println(s"${vocabulary.wordMap.getOrElse(token, "***")} $outO")
        set + token
    }

  println(" ====== From generalization:")
  calc(vocabulary.map2ToMiddle.collect {
    case record@(w1 :: w3 :: _, ws) if !(ws.map(_._2).exists(x => vocabulary.sense(x) || vocabulary.auxiliary(x))) =>
      record
  })
    .foreach {
      case (token, outO) =>
        if (!(sampledTokens contains token))
          println(s"${vocabulary.wordMap.getOrElse(token, "***")} $outO")
    }

  system.shutdown()
}
