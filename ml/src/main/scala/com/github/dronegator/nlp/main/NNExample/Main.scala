package com.github.dronegator.nlp.main.NNExample

import breeze.linalg.{DenseMatrix, DenseVector, SparseVector, Vector}
import com.github.dronegator.nlp.main.NNExample.GrammarNetwork.{Err, Grad, In, Out}

/**
  * Created by cray on 11/28/16.
  */

trait NeuralNetwork[A, B, E, G] {

  def feedforward(a: A): B

  def backpropagation(e: E, a: A): G

  def changeweights(g: G)
}

case class Gradient(klassenToOut: DenseMatrix[Double], inToKlassen: DenseMatrix[Double])

object GrammarNetwork {
  type In = SparseVector[Double]
  type Out = SparseVector[Double]
  type Err = Out
  type Grad = Gradient
}

class GrammarNetwork(nTokens: Int, nKlassen: Int, nOut: Int)
  extends NeuralNetwork[In, Out, Err, Grad] {

  val InToKlassen = DenseMatrix.zeros[Double](nKlassen, nTokens)

  val KlassenI: Vector[Double] = DenseVector.zeros(nKlassen * 2)

  val KlassenO: Vector[Double] = DenseVector.zeros(nKlassen * 2)

  val KlassenToOut = DenseMatrix.zeros[Double](1, nKlassen * 2)

  override def feedforward(a: In): Out = {
    KlassenI(0 until nKlassen) := InToKlassen * a(0 until nTokens)
    KlassenI(nKlassen until nKlassen * 2) := InToKlassen * a(nTokens until nTokens * 2)

    KlassenO := KlassenI.map(x => 1 / (1 + scala.math.exp(-x)))

    SparseVector.zeros[Double](nOut) := (KlassenToOut * KlassenO)
  }

  override def backpropagation(e: Err, a: In): Grad = ???

  override def changeweights(g: Grad): Unit = {
    InToKlassen += g.inToKlassen

    KlassenToOut += g.klassenToOut
  }
}


object Main {

}
