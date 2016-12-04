package com.github.dronegator.nlp.main.NNExample

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector}
import breeze.numerics._
import breeze.optimize.DiffFunction

import scala.util.Random

/**
  * Created by cray on 11/28/16.
  */


class NN(nKlassen: Int, nToken: Int, dropout: Int, sample: => Iterator[(SparseVector[Double], SparseVector[Double])])
  extends DiffFunction[DenseVector[Double]] {

  def initial = DenseVector.rand[Double](nKlassen * nToken + nKlassen * 2)

  def network(vector: DenseVector[Double]) = {
    val termToKlassen: DenseMatrix[Double] = vector(0 until nToken * nKlassen).asDenseMatrix.reshape(nKlassen, nToken)

    val klassenToOut: DenseMatrix[Double] = vector(nToken * nKlassen until (nToken * nKlassen + nKlassen * 2)).asDenseMatrix.reshape(1, nKlassen * 2)

    (termToKlassen, klassenToOut)
  }

  override def calculate(vector: DenseVector[Double]): (Double, DenseVector[Double]) = {

    val (termToKlassen, klassenToOut) = network(vector)

    val indexes = (0 until nKlassen * 2)
      .collect {
        case i if Random.nextInt(100) > dropout =>
          i
      }

    sample
      .map {
        case (input, output) =>

          val klassenI = DenseVector.zeros[Double](2 * nKlassen)

          klassenI(0 until nKlassen) := termToKlassen * input(0 until nToken)

          //println(input(nToken until nToken * 2).size, input.size, nToken, "input2")

          klassenI(nKlassen until nKlassen * 2) := termToKlassen * input(nToken until nToken * 2)

          //          if (dropout > 0)
          //            for (i <- 0 until nKlassen * 2) {
          //              if (Random.nextInt(100) <= dropout) {
          //                klassenI(i) = 0.0
          //              }
          //            }

          for (i <- indexes) {
            klassenI(i) = 0.0
          }

          val klassenO = klassenI.map(x => 1 / (1 + exp(-x)))

          val outI = DenseVector.zeros[Double](1)

          outI := klassenToOut * klassenO

          val outO = outI.map(x => 1 / (1 + exp(-x)))

          val value = (outO - output).norm()

          //println(value, input, output)

          val gradient: DenseVector[Double] = initial

          val (gTermToKlassen, gKlassen2Out) = network(gradient)

          val backOutI = outO * 2.0 :* (outO :* (-outO + 1.0))

          gKlassen2Out := (backOutI * klassenO.t)

          val backKlassenO = klassenToOut.t * backOutI

          val backKlassenI = backKlassenO :* klassenO :* (-klassenO + 1.0)

          gTermToKlassen := (backKlassenI(0 until nKlassen) * input(0 until nToken).toDenseVector.t) +
            (backKlassenI(nKlassen until 2 * nKlassen) * input(nToken until (2 * nToken)).toDenseVector.t)

          (value, gradient)
      }
      .reduce[(Double, DenseVector[Double])] {
      case ((value1, gradient1), (value2, gradient2)) =>
        ((value1 + value2), (gradient1 + gradient2))
    }
  }
}
