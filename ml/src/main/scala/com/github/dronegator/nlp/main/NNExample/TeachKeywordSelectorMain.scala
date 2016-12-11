package com.github.dronegator.nlp.main.NNExample

import java.io._

import breeze.linalg.{DenseVector, SparseVector}
import breeze.numerics._
import breeze.optimize.{AdaDeltaGradientDescent, DiffFunction}
import breeze.util.Implicits._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.main.{Concurent, MainConfig, MainTools}
import com.github.dronegator.nlp.trace._
import com.github.dronegator.nlp.utils.Match._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyImpl}
import com.typesafe.scalalogging.LazyLogging
import configs.syntax._

import scala.util.{Random, Try}

object TeachKeywordSelectorMain
  extends App
    with MainTools
    with MainConfig[NLPTAppForNNConfig]
    with Concurent
    with LazyLogging
    with NLPTAppForNN {

  val fileIn :: OptFile(matrix) = args.toList

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

  lazy val (sense, crossValidationSense) = vocabulary.sense.partition(_ => Random.nextInt(100) > cfg.crossValidationWords)
  lazy val (auxiliary, crossValidationAuxiliary) = vocabulary.auxiliary.partition(_ => Random.nextInt(100) > cfg.crossValidationWords)

  lazy val (samples, crossValidationSamples) = vocabulary.nGram3.keysIterator
    .map {
      case ws@w1 :: w2 :: w3 :: _ if ws.forall(_ <= nToken) =>
        lazy val input = (w1, w3)

        if (sense contains w2)
          Some(input -> SparseVector(1)(0 -> 1.0))
        else if (auxiliary contains w2)
          Some(input -> SparseVector(1)(0 -> 0.0))
        else None
    }
    .collect {
      case Some(sample) => sample
    }
    .toList
    .partition(_ => Random.nextInt(100) > cfg.crossValidationContext)

  lazy val crossWordValidationSamples = vocabulary.nGram3.keysIterator
    .map {
      case ws@w1 :: w2 :: w3 :: _ if ws.forall(_ <= nToken) =>
        lazy val input = (w1, w3)

        if (crossValidationSense contains w2)
          Some(input -> SparseVector(1)(0 -> 1.0))
        else if (crossValidationAuxiliary contains w2)
          Some(input -> SparseVector(1)(0 -> 0.0))
        else None
    }
    .collect {
      case Some(sample) => sample
    }
    .toList

  val lbfgs = //if (cfg.useLBFGS)
  //new LBFGS[DenseVector[Double]](maxIter = cfg.maxIter, m = cfg.memoryLimit, tolerance = cfg.tolerance)
  //  else
    new AdaDeltaGradientDescent[DenseVector[Double]](rho = cfg.rfo, maxIter = cfg.maxIter, tolerance = cfg.tolerance)
  //new SimpleSGD[DenseVector[Double]](maxIter = cfg.maxIter)

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

  val nn = new NN(cfg.nKlassen, nToken, cfg.dropout, cfg.winnerGetsAll, cfg.nSample.map(samples.toIterator.take(_)).getOrElse(samples.toIterator))
  val nnCrossValidation = new NN(cfg.nKlassen, nToken, 0, cfg.winnerGetsAll, cfg.nSample.map(crossValidationSamples.toIterator.take(_)).getOrElse(crossValidationSamples.toIterator))
  val nnCrossWordValidation = new NN(cfg.nKlassen, nToken, 0, cfg.winnerGetsAll, cfg.nSample.map(crossWordValidationSamples.toIterator.take(_)).getOrElse(crossWordValidationSamples.toIterator))

  //  val network = lbfgs.minimize(nn, DenseVector.rand(2*10+10*2))

  val initial =
    for {
      matrix <- matrix
      initial <- Try {
        val file = new ObjectInputStream(new FileInputStream(matrix))
        val nn = file.readObject().asInstanceOf[DenseVector[Double]]
        file.close()
        nn
      }.toOption

    } yield initial

  //  val network = initial.get
  val network = lbfgs.iterations(
    if (cfg.regularization < 0.000001) nn else DiffFunction.withL2Regularization(nn, cfg.regularization),
    initial getOrElse (((nn.initial :* 2.0) :- 1.0) :* cfg.range))
    .map {
      case x =>
        val crossValue = nnCrossValidation.calculate(x.x)._1
        val crossWordValue = nnCrossWordValidation.calculate(x.x)._1
        val value = nn.calculate(x.x)._1

        println(f"value=${math.sqrt(x.value)}%8.6f value=${math.sqrt(value)}%8.6f cross=${math.sqrt(crossValue)}%8.6f crossword=${math.sqrt(crossWordValue)}%8.6f")
        matrix.foreach { matrix =>
          val file = new ObjectOutputStream(new FileOutputStream(matrix))
          file.writeObject(x.x)
          file.close()
        }
        x.x
    }
    .last

  println("stop")

  //  nn.calculate(network)
  //
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
        println(f"${
          vocabulary.wordMap(token)
        }%10s $weight")

    }
  }

  println(s"====== ==")
  implicit val orderingDenseVectorDouble = Ordering.fromLessThan[DenseVector[Double]] {
    (x, y) =>
      (x :< y).forall(x => x)
  }

  def calc(samples: Iterable[(List[Token], List[(Double, Token)])]) =
    samples.collect {
      case (w1 :: w3 :: _, ws) =>
        val klassenI = DenseVector.zeros[Double](2 * cfg.nKlassen)

        klassenI(0 until cfg.nKlassen) := termToKlassen(::, w1) * 1.0

        klassenI(cfg.nKlassen until cfg.nKlassen * 2) := termToKlassen(::, w3) * 1.0

        val klassenO = klassenI.map(x => 1 / (1 + exp(-x)))

        val outI = DenseVector.zeros[Double](1)

        outI := klassenToOut * klassenO

        val outO = outI.map(x => 1 / (1 + exp(-x)))
        println(s" ------------> ($w1 x $w3) $outO $outI ${klassenO} ${vocabulary.wordMap.get(ws.head._2)}")
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

  println(" ===== Main sample words")
  calc(vocabulary.nGram3.keysIterator.collect {
    case w1 :: w2 :: w3 :: _ if vocabulary.sense(w2) || vocabulary.auxiliary(w2) =>
      (w1 :: w3 :: Nil, List((0.0, w2)))
  }.toIterable)
    .map {
      case (token, out) =>
        println(s"${
          vocabulary.wordMap.getOrElse(token, "***")
        } $out ${sense(token)} ${crossValidationSense(token)} ${auxiliary(token)} ${crossValidationAuxiliary(token)}")
    }

  println(" ====== From samples:")

  val sampledTokens = calc(vocabulary.map2ToMiddle.collect {
    case record@(w1 :: w3 :: _, ws) if ws.map(_._2).exists(x => vocabulary.sense(x) || vocabulary.auxiliary(x)) =>
      record
  })
    .foldLeft(Set[Token]()) {
      case (set, (token, outO)) =>
        println(s"${
          vocabulary.wordMap.getOrElse(token, "***")
        } $outO")
        set + token
    }

  println(" ====== From generalization:")
  calc(vocabulary.map2ToMiddle.collect {
    case record@(w1 :: w3 :: _, ws) if !(ws.map(_._2).exists(x => vocabulary.sense(x) || vocabulary.auxiliary(x))) => // && ((Random.nextInt() % 10) != 0)=>
      record
  })
    .foreach {
      case (token, outO) =>
        if (!(sampledTokens contains token))
          println(s"${
            vocabulary.wordMap.getOrElse(token, "***")
          } $outO")
    }

  println(" ====== The end:")
  system.shutdown()
}
