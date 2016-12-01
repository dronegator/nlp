package com.github.dronegator.nlp.main.NNExample

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector}
import breeze.numerics._
import breeze.optimize.DiffFunction

/**
  * Created by cray on 11/28/16.
  */


class NN(nKlassen: Int, nToken: Int, sample: => Iterator[(SparseVector[Double], SparseVector[Double])])
  extends DiffFunction[DenseVector[Double]] {


  override def calculate(vector: DenseVector[Double]): (Double, DenseVector[Double]) = {
    val termToKlassen: DenseMatrix[Double] = vector(0 until nToken * nKlassen).asDenseMatrix.reshape(nKlassen, nToken)
    println(nToken, nKlassen)
    val klassenToOut: DenseMatrix[Double] = vector(nToken * nKlassen until (nToken * nKlassen + nKlassen * 2)).asDenseMatrix.reshape(1, nKlassen * 2)

    sample
      .map {
        case (input, output) =>

          val klassenI = DenseVector.zeros[Double](2 * nKlassen)

          klassenI(0 until nKlassen) := termToKlassen * input(0 until nToken)

          println(input(nToken until nToken * 2).size, input.size, nToken, "input2")

          klassenI(nKlassen until nKlassen * 2) := termToKlassen * input(nToken until nToken * 2)

          val klassenO = klassenI.map(x => 1 / (1 + exp(-x)))

          val outI = DenseVector.zeros[Double](1)

          outI := klassenToOut * klassenO

          val outO = outI.map(x => 1 / (1 + exp(-x)))

          val value = sqrt((outO - output) dot (outO - output))

          println(value, input, output)

          val gradient: DenseVector[Double] = ???

          (value, gradient)
      }
      .reduce[(Double, DenseVector[Double])] {
      case ((value1, gradient1), (value2, gradient2)) =>
        ((value1 + value2), (gradient1 + gradient2))
    }
  }
}
