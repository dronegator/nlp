package com.github.dronegator.nlp.main

import java.io.{FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream}

import breeze.linalg.DenseVector
import breeze.optimize.StochasticGradientDescent.SimpleSGD
import breeze.optimize._
import breeze.util.Implicits._
import com.github.dronegator.nlp.utils.Match._
import com.typesafe.scalalogging.LazyLogging
import enumeratum.EnumEntry.Lowercase
import enumeratum.{EnumEntry, _}

import scala.util.{Random, Try}

/**
  * Created by cray on 12/12/16.
  */

sealed trait Algorithm extends EnumEntry with Lowercase

object Algorithm extends Enum[Algorithm] {
  override def values: Seq[Algorithm] = findValues

  case object LBFGS extends Algorithm

  case object AdaDeltaGradientDescent extends Algorithm

  case object L1Regularization extends Algorithm

  case object L2Regularization extends Algorithm

  case object SimpleSGD extends Algorithm

}

trait MLCfg {
  val crossvalidationRatio: Int
  val algorithm: Algorithm
  val regularization: Double
  val range: Double
  val maxIter: Int
  val useLBFGS: Boolean
  val rfo: Double
  val tolerance: Double
  val memoryLimit: Int
  val dropout: Int
  val crossvalidationFrequency: Int
}

trait NLPTAppMlTools[C <: MLCfg, I, O] {

  self: App with MainConfig[C] =>

  def sampling: Iterable[(I, O)]

  def cfg: C

  val fileIn :: OptFile(matrix) = args.toList

  println(cfg)

  lazy val (samplingLearn, samplingCross) =
    sampling.partition(_ => Random.nextInt(100) < cfg.crossvalidationRatio)


  lazy val algorithmIterator =
    cfg.algorithm match {
      case Algorithm.LBFGS =>
        new LBFGS[DenseVector[Double]](maxIter = cfg.maxIter, m = cfg.memoryLimit, tolerance = cfg.tolerance)
          .iterations(nnR, initial)

      case Algorithm.AdaDeltaGradientDescent =>
        new AdaDeltaGradientDescent[DenseVector[Double]](rho = cfg.rfo, maxIter = cfg.maxIter, tolerance = cfg.tolerance)
          .iterations(nnR, initial)

      case Algorithm.L1Regularization =>
        new AdaptiveGradientDescent.L1Regularization[DenseVector[Double]]()
          .iterations(nn, initial)

      case Algorithm.L2Regularization =>
        new AdaptiveGradientDescent.L2Regularization[DenseVector[Double]](stepSize = 1.0, maxIter = cfg.maxIter)
          .iterations(nn, initial)

      case Algorithm.SimpleSGD =>
        new SimpleSGD[DenseVector[Double]](maxIter = cfg.maxIter)
          .iterations(nn, initial)
    }

  def nn: DiffFunction[DenseVector[Double]]

  def nnR = if (cfg.regularization < 0.00000001) nn else DiffFunction.withL2Regularization(nn, cfg.regularization)

  def nnCross: DiffFunction[DenseVector[Double]]

  def nnDoubleCross: DiffFunction[DenseVector[Double]]

  def genInitial: DenseVector[Double]

  def initial =
    (for {
      matrix <- matrix
      initial <- Try {
        val file = new ObjectInputStream(new FileInputStream(matrix))
        val nn = file.readObject().asInstanceOf[DenseVector[Double]]
        file.close()
        nn
      }.toOption

    } yield initial) getOrElse (((genInitial :* 2.0) :- 1.0) :* cfg.range)

  lazy val network = algorithmIterator
    .scanLeft((0, Option.empty[DenseVector[Double]])) {
      case ((n, _), x) =>
        val value = nn.calculate(x.x)._1

        if ((n + 1) % cfg.crossvalidationFrequency == 0) {
          val valueCross = nnCross.calculate(x.x)._1
          val valueDoubleCross = nnDoubleCross.calculate(x.x)._1
          println(f"value=${math.sqrt(x.value)}%8.6f value=${math.sqrt(value)}%8.6f cross_value=${math.sqrt(valueCross)}%8.6f double_cross_value=${math.sqrt(valueDoubleCross)}%8.6f")
        } else {
          println(f"value=${math.sqrt(x.value)}%8.6f value=${math.sqrt(value)}%8.6f ")
        }

        matrix.foreach { matrix =>
          val file = new ObjectOutputStream(new FileOutputStream(matrix))
          file.writeObject(x.x)
          file.close()
        }

        (n + 1, Some(x.x))
    }
    .collect {
      case (_, Some(x)) => x
    }
    .last

  def calc(sampling: Iterable[(I, O)]): Unit

  def report: Unit = {
    // print interesting options of the network itself
    network

    // print a few specific examples
  }


}


trait NLPTAppMl[C]
  extends App
    with MainTools
    with MainConfig[C]
    with Concurent
    with LazyLogging {
}
