package com.github.dronegator.nlp.main.NNExample

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector}
import breeze.numerics._
import breeze.optimize.DiffFunction
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token

import scala.util.Random

/**
  * Created by cray on 11/28/16.
  */


class NN(nKlassen: Int, nToken: Int, dropout: Int, winnerGetsAll: Boolean, sample: => Iterator[((Token, Token), SparseVector[Double])])
  extends DiffFunction[DenseVector[Double]] {

  val t = System.currentTimeMillis()

  def initial = DenseVector.rand[Double](nKlassen * nToken + nKlassen * 2)

  def initialZ = DenseVector.zeros[Double](nKlassen * nToken + nKlassen * 2)

  def network(vector: DenseVector[Double]) = {
    val termToKlassen: DenseMatrix[Double] = vector(0 until nToken * nKlassen).asDenseMatrix.reshape(nKlassen, nToken)

    val klassenToOut: DenseMatrix[Double] = vector(nToken * nKlassen until (nToken * nKlassen + nKlassen * 2)).asDenseMatrix.reshape(1, nKlassen * 2)

    (termToKlassen, klassenToOut)
  }

  override def calculate(vector: DenseVector[Double]): (Double, DenseVector[Double]) = {

    val (termToKlassen, klassenToOut) = network(vector)

    def indexes = (0 until nKlassen * 2)
      .collect {
        case i if Random.nextInt(100) < dropout =>
          i
      }

    val gradient: DenseVector[Double] = initialZ

    var yes: Int = 0
    var no: Int = 0

    val value = sample
      .map {
        case ((in1, in2), output) =>

          val klassenI = DenseVector.zeros[Double](2 * nKlassen)

          klassenI(0 until nKlassen) := termToKlassen(::, in1) * 0.1 //input(0 until nToken)

          //println(input(nToken until nToken * 2).size, input.size, nToken, "input2")

          klassenI(nKlassen until nKlassen * 2) := termToKlassen(::, in2) * 0.1 //input(nToken until nToken * 2)

          val klassenO = klassenI.map(x => 1 / (1 + exp(-x)))

          for (i <- indexes) {
            klassenO(i) = 0.0
          }

          if (winnerGetsAll) {
            val (i1x, v1x) = klassenO(0 until nKlassen).iterator.maxBy(_._2)

            val (i2x, v2x) = klassenO(nKlassen until nKlassen * 2).iterator.maxBy(_._2)

            val (i1n, v1n) = klassenO(0 until nKlassen).iterator.minBy(_._2)

            val (i2n, v2n) = klassenO(nKlassen until nKlassen * 2).iterator.minBy(_._2)

            klassenO := 0.0

            klassenO(i1x) = v1n

            klassenO(i2x) = v2n

            klassenO(i1n) = v1n

            klassenO(i2n) = v2n
          }

          val outI = DenseVector.zeros[Double](1)

          outI := klassenToOut * klassenO

          val outO = outI.map(x => 1 / (1 + exp(-x)))

          val value = (outO - output).norm()

          val (gTermToKlassen, gKlassen2Out) = network(gradient)

          val backOutI = (outO - output) * 2.0 :* (outO :* (-outO + 1.0))

          gKlassen2Out :+= (backOutI * klassenO.t)

          val backKlassenO = klassenToOut.t * backOutI

          val backKlassenI = backKlassenO :* klassenO :* (-klassenO + 1.0)

          gTermToKlassen(::, in1) :+= (backKlassenI(0 until nKlassen) * 1.0) /*input(0 until nToken).toDenseVector.t)*/

          gTermToKlassen(::, in2) :+= (backKlassenI(nKlassen until 2 * nKlassen) * 1.0) //input(nToken until (2 * nToken)).toDenseVector.t)

          value
      }
      .scanLeft((0, (0.0))) {
        case ((i, _), x) =>
          if (i % 100000 == 0)
            println(f"${(System.currentTimeMillis() - t) / 1000}%8d $i%8d $x $i")
          if (x > 0.9) {
            yes += 1
          }

          if (x < 0.1) {
            no += 1
          }

          if (x > 1) {
            println(x)
          }
          (i + 1, x)
      }
      .drop(1)
      .map {
        _._2
      }
      .reduce(_ + _)
    //println(s"|grad| = ${gradient.norm()}")
    println(yes, no, sample.size)
    (value / sample.size, gradient :/ sample.size.toDouble)
  }
}
