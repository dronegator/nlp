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
  val rfo: Double
  val tolerance: Double
  val memoryLimit: Int
  val dropout: Int
  val crossvalidationFrequency: Int
  val lambda: Double
  val delta: Double
  val eta: Double
  val stepSize: Double
  val minImprovementWindow: Int
  val learn: Boolean
  val windowRate: Int
}

trait NLPTAppMlTools[C <: MLCfg, I, O, N] {

  self: App with MainConfig[C] =>

  def sampling: Iterable[(I, O)]

  def samplingDoubleCross: Iterable[(I, O)]

  def cfg: C

  lazy val Switches(switches, fileIn :: OptFile(matrix)) = {
    println(args)
    args.toList
  }

  lazy val (samplingLearn, samplingCross) =
    sampling.partition(_ => Random.nextInt(100) >= cfg.crossvalidationRatio)

  lazy val algorithmIterator =
    cfg.algorithm match {
      case Algorithm.LBFGS =>
        new LBFGS[DenseVector[Double]](maxIter = cfg.maxIter, m = cfg.memoryLimit, tolerance = cfg.tolerance)
          .iterations(nnR, initial)

      case Algorithm.AdaDeltaGradientDescent =>
        new AdaDeltaGradientDescent[DenseVector[Double]](
          rho = cfg.rfo,
          maxIter = cfg.maxIter,
          tolerance = cfg.tolerance,
          minImprovementWindow = cfg.minImprovementWindow)
          .iterations(nnR, initial)

      case Algorithm.L1Regularization =>
        new AdaptiveGradientDescent.L1Regularization[DenseVector[Double]](
          lambda = cfg.lambda,
          delta = cfg.delta,
          eta = cfg.eta,
          maxIter = cfg.maxIter)
          .iterations(nn, initial)

      case Algorithm.L2Regularization =>
        new AdaptiveGradientDescent.L2Regularization[DenseVector[Double]](
          regularizationConstant = cfg.regularization,
          stepSize = cfg.stepSize, maxIter = cfg.maxIter,
          tolerance = cfg.stepSize, minImprovementWindow = cfg.minImprovementWindow)
          .iterations(nn, initial)

      case Algorithm.SimpleSGD =>
        new SimpleSGD[DenseVector[Double]](maxIter = cfg.maxIter)
          .iterations(nn, initial)
    }

  def nn: NNSampleTrait[I, O, N, _, _] with DiffFunction[DenseVector[Double]]

  def nnR = if (cfg.regularization < 0.00000001) nn else DiffFunction.withL2Regularization(nn, cfg.regularization)

  def nnCross: DiffFunction[DenseVector[Double]]

  def nnDoubleCross: DiffFunction[DenseVector[Double]]

  def genInitial: DenseVector[Double] = nn.initial

  def initial =
    (for {
      matrix <- matrix
      initial <- Try {
        val file = new ObjectInputStream(new FileInputStream(matrix))
        val nn = file.readObject().asInstanceOf[NNForw[I, O, _, N]]
        file.close()
        nn.vector
      }.toOption

    } yield initial) getOrElse (((genInitial :* 2.0) :- 1.0) :* cfg.range)

  lazy val network = {
    if (cfg.learn) {
      println(s"sampling size =               ${samplingLearn.size}")
      println(s"cross sampling size =         ${samplingCross.size}")
      println(s"double cross sampling size =  ${samplingDoubleCross.size}")

      algorithmIterator
        .scanLeft((0, Option.empty[DenseVector[Double]])) {
          case ((n, _), x) =>
            val value = x.value //nn.calculate(x.x)._1

            if (n % cfg.crossvalidationFrequency == 0) {
              val valueCross = nnCross.calculate(x.x)._1
              val valueDoubleCross = nnDoubleCross.calculate(x.x)._1
              println(f"${n}%7d value=${math.sqrt(x.value)}%8.6f cross_value=${math.sqrt(valueCross)}%8.6f double_cross_value=${math.sqrt(valueDoubleCross)}%8.6f")
            } else {
              println(f"${n}%7d value=${math.sqrt(x.value)}%8.6f")
            }

            matrix.foreach { matrix =>
              val file = new ObjectOutputStream(new FileOutputStream(matrix))
              file.writeObject(nn.net(x.x))
              file.close()
            }

            (n + 1, Some(x.x))
        }
        .collect {
          case (_, Some(x)) =>
            x
        }
        .last
    }
    else {
      println("Do not learn")
      initial
    }
  }

  def printNetwork(network: N): Unit

  def calc(sampling: Iterable[(I, O)]): Unit

  def report: Unit = {
    // print interesting options of the network itself
    println("== The Network:")
    printNetwork(nn.network(network))

    // print a few specific examples
    println("== The outcome on the Sampling:")
    calc(samplingLearn)

    println("== The outcome on the Crosssampling:")
    calc(samplingCross)

    println("== The outcome on the Doublecrosssampling:")
    calc(samplingDoubleCross)
  }


}


trait NLPTAppMl[C]
  extends App
    with MainTools
    with MainConfig[C]
    with Concurent
    with LazyLogging {
}
