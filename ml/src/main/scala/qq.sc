import breeze.linalg._

object Q {
  val x = breeze.linalg.Matrix.rand(2, 2)
  val y = breeze.linalg.Matrix.rand(2, 2)

  val z = Matrix.zeros[Double](2, 2)

  z := x + y

  x
  y
  z

  val qq: Matrix[Double] = z * x

  qq


}