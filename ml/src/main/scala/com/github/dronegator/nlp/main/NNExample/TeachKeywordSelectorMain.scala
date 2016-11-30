package com.github.dronegator.nlp.main.NNExample

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector}
import breeze.numerics.{exp, sqrt}
import breeze.optimize.{DiffFunction, LBFGS}
import breeze.util.Implicits._

/**
  * Created by cray on 11/28/16.
  */


class NN(nKlassen: Int, nTerms: Int, sample: Iterator[(SparseVector[Double], SparseVector[Double])])
  extends DiffFunction[DenseVector[Double]] {


  override def calculate(vector: DenseVector[Double]): (Double, DenseVector[Double]) = {
    val termToKlassen: DenseMatrix[Double] = vector(0 until nTerms * nKlassen).asDenseMatrix.reshape(nTerms, nKlassen)

    val klassenToOut: DenseMatrix[Double] = vector(nTerms * nKlassen to -1).asDenseMatrix.reshape(nKlassen * 2, 1)

    sample
      .map {
        case (input, output) =>

          val klassenI = DenseVector.zeros[Double](2 * nKlassen)

          klassenI(0 until nKlassen) := termToKlassen * input(0 until nTerms)

          klassenI(nKlassen to -1) := termToKlassen * input(nTerms to -1)

          val klassenO = klassenI.map(x => 1 / (1 + exp(-x)))

          val outI = DenseVector.zeros[Double](1)

          outI := klassenToOut * klassenO

          val outO = outI.map(x => 1 / (1 + exp(-x)))

          val value = sqrt((outO - output) dot (outO - output))

          val gradient: DenseVector[Double] = ???

          (value, gradient)
      }
      .reduce {
        case ((value1, gradient1), (value2, gradient2)) =>
          ((value1 + value2), (gradient1 + gradient2))
      }
  }
}

object TeachKeywordSelectorMain {


  val lbfgs = new LBFGS[DenseVector[Double]]()

  val nn = new NN(2, 10, ???)

  //  val network = lbfgs.minimize(nn, DenseVector.rand(2*10+10*2))

  val network = lbfgs.iterations(nn, DenseVector.rand(2 * 10 + 10 * 2))
    .map {
      case x =>
        println(x.value)
        x.x
    }
    .last
}
