package com.github.dronegator.nlp.main

import breeze.linalg.DenseVector
import breeze.optimize.DiffFunction

/**
  * Created by cray on 12/13/16.
  */
trait NNQuality[O, Q] {
  def quality(quality: Q, result: O): Q

  def quality: Q

  def report(quality: Q) =
    println(quality)
}

trait NNCalc[O] {
  def error(output: O, result: O): Double
}

trait NNCalcDenseVector
  extends NNCalc[DenseVector[Double]] {
  override def error(output: DenseVector[Double], result: DenseVector[Double]): Double = {
    val v = (result - output)
    v dot v
  }
}

trait NN[I, O, H] {
  def forward(hidden: H, input: I): O

  def hiddenInit(): H

  def forward(input: I): O =
    forward(hiddenInit(), input)
}

trait NNSampleTrait[I, O, N, H, Q] {
  self: DiffFunction[DenseVector[Double]]
    with NNQuality[O, Q]
    with NNCalc[O]
    with NN[I, O, H] =>

  lazy val startTime = System.currentTimeMillis()

  def network(vector: DenseVector[Double]): N

  def size: Int

  def empty = DenseVector.zeros[Double](size)

  def initial = DenseVector.zeros[Double](size)

  def sampling: Iterable[(I, O)]

  def backward(hidden: H, input: I, output: O, result: O): O


  override def calculate(vector: DenseVector[Double]): (Double, DenseVector[Double]) = {

    val nn = network(vector)

    val (n, accumulatedValue, gradient, qualityValue) = sampling
      .foldLeft((1, 0.0, empty, quality)) {
        case ((n, accumulatedValue, gradient, qualityValue), (input, output)) =>

          val hidden: H = hiddenInit()

          val result = forward(hidden, input)

          val value = error(output, result)

          backward(hidden, input, output, result)

          if (n % 10000 == 0) {
            println(f"${(System.currentTimeMillis() - startTime) / 1000}%8d $n%8d $output $result")
          }

          (n, accumulatedValue + value, gradient, quality(qualityValue, result))
      }

    report(qualityValue)
    (math.sqrt(accumulatedValue) / n, gradient :/ n.toDouble)
  }
}
