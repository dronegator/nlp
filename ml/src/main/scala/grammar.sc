/*
* x - keyword or not
* y - classes
* z - digramms
*
*
*
* x = Q1 * ( Y1 Y2 )
* Y1 = Y * Z1
* Y2 = Y * Z2
*
*
 */

import breeze.linalg._

object Grammar {
  val nTokens = 10
  val nKlassen = 2
  val t1 = 2
  val t2 = 4

  val Z1 = breeze.linalg.SparseVector[Double](nTokens)((t1, 1.0))
  val Z2 = breeze.linalg.SparseVector(nTokens)((t2, 1.0))

  Z1
  Z2

  val Y = breeze.linalg.Matrix.zeros[Double](nKlassen, nTokens)

  Y := 1.0

  val YB: Vector[Double] = Vector.zeros(nKlassen * 2)

  YB(0 until nKlassen) := Y * Z1

  YB(nKlassen until 2 * nKlassen) := Y * Z2

  YB

  val YtoX = breeze.linalg.Matrix.zeros[Double](1, nKlassen * 2)

  YtoX := 1.0

  val X = YtoX * YB
}