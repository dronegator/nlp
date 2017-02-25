package com.github.dronegator.nlp.main

import breeze.linalg.{DenseVector, SparseVector}
import breeze.optimize.DiffFunction

import scala.util.Random

/**
  * Created by cray on 12/13/16.
  */
trait NNQuality[O, Q] {
  def quality(quality: Q, result: O): Q

  def quality: Q

  def report(quality: Q) =
    println(s"        quality = $quality")
}

trait NNCalc[O] {
  def error(output: O, result: O): Double
}

trait NNCalcDenseVector
  extends NNCalc[DenseVector[Double]] {
  override def error(output: DenseVector[Double], result: DenseVector[Double]): Double = {
    val v = (result - output)
    (v dot v) / 2.0 / v.iterableSize
  }
}

trait NNCalcSparseVector
  extends NNCalc[SparseVector[Double]] {
  override def error(output: SparseVector[Double], result: SparseVector[Double]): Double = {
    val v = (result - output)
    (v dot v) / 2.0 / v.iterableSize
  }
}

trait NN[I, O, H, N] {
  def forward(n: N, hidden: H, input: I): O

  def hiddenInit(): H

  def forward(n: N, input: I): O =
    forward(n: N, hiddenInit(), input)
}

trait NNSampleTrait[I, O, N, H, Q] {
  self: DiffFunction[DenseVector[Double]]
    with NNQuality[O, Q]
    with NNCalc[O]
    with NN[I, O, H, N] =>

  lazy val startTime = System.currentTimeMillis()

  def network(vector: DenseVector[Double]): N

  def size: Int

  def rate: Int

  def empty = DenseVector.zeros[Double](size)

  def initial = DenseVector.rand[Double](size)

  def sampling: Iterable[(I, O)]

  def backward(nn: N, gradient: N, hidden: H, input: I, output: O, result: O): Unit

  override def calculate(vector: DenseVector[Double]): (Double, DenseVector[Double]) = {

    val nn = network(vector)

    val gradient11 = empty

    val (n, accumulatedValue, _, qualityValue, _) = sampling
      .filter(_ => Random.nextInt(100) < rate)
      .foldLeft((0, 0.0, network(gradient11), quality, System.currentTimeMillis())) {
        case ((n, accumulatedValue, gradient, qualityValue, lastTime), (input, output)) =>

          val hidden: H = hiddenInit()

          val result = forward(nn, hidden, input)

          val value = error(output, result)

          backward(nn, gradient, hidden, input, output, result)

          val currentTime = System.currentTimeMillis()

          val nextTime = if (currentTime - lastTime > 1000) {
            println(f"${(currentTime - startTime) / 1000}%8d $n%8d" /*"$output $result"*/)
            currentTime
          } else {
            lastTime
          }

          //          println(s"accumulatedValue=$accumulatedValue value=$value")
          (n + 1, accumulatedValue + value, gradient, quality(qualityValue, result), nextTime)
      }


    //println(s"---> $accumulatedValue $n ${network(gradient11).asInstanceOf[com.github.dronegator.nlp.main.chain.NNSampleChain.Network].tokenToKlassen.t}")
    report(qualityValue)
    (math.sqrt(accumulatedValue / n), gradient11 :/ n.toDouble)
  }
}
