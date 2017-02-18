import shapeless._
import shapeless.ops.hlist._


trait ShapeZipLowest {

  def apply[A, B, C](implicit shapeZip: ShapeZip[A, B, C]) =
    shapeZip

  def createShapeZip[A, B, C](f: (Seq[A], Seq[B]) => Seq[C]) =
    new ShapeZip[A, B, C] {
      override def shapeZip(as: Seq[A], bs: Seq[B]): Seq[C] =
        f(as, bs)
    }

  implicit def shapeZipAnyAny[A, B](implicit
                                    aNotAProduct: A <:!< Product,
                                    bNotAProduct: B <:!< Product) =
    createShapeZip[A, B, (A, B)] { (as, bs) =>
      as.zip(bs)
    }

  implicit def shapeZipAnyTuple[A, B, BRepl <: HList, C](implicit
                                                         aNotAProduct: A <:!< Product,
                                                         genB: Generic.Aux[B, BRepl],
                                                         tupler: Tupler.Aux[A :: BRepl, C]) =
    createShapeZip[A, B, C] { (as, bs) =>
      as.zip(bs).map {
        case (a, b) =>
          tupler(a :: genB.to(b))
      }
    }

  implicit def shapeZipTupleAny[A, ARepl <: HList, B, C, CRepl <: HList](implicit
                                                                         bNotAProduct: B <:!< Product,
                                                                         genA: Generic.Aux[A, ARepl],
                                                                         prepend: Prepend.Aux[ARepl, B :: HNil, CRepl],
                                                                         tupler: Tupler.Aux[CRepl, C]) =
    createShapeZip[A, B, C] { (as, bs) =>
      as.zip(bs).map {
        case (a, b) =>
          tupler(genA.to(a) :+ b)
      }
    }

  implicit def shapeZipTupleTuple[A, ARepl <: HList, B, BRepl <: HList, C, CRepl <: HList](implicit
                                                                                           genA: Generic.Aux[A, ARepl],
                                                                                           genB: Generic.Aux[B, BRepl],
                                                                                           concat: Prepend.Aux[ARepl, BRepl, CRepl],
                                                                                           tupler: Tupler.Aux[CRepl, C]) =
    createShapeZip[A, B, C] { (as, bs) =>
      as.zip(bs).map {
        case (a, b) =>
          tupler(genA.to(a) ++ genB.to(b))
      }
    }

}

trait ShapeZipHigh
  extends ShapeZipLowest {
}


object ShapeZip
  extends ShapeZipHigh {

  implicit class ShapeZipOps[A](as: Seq[A]) {
    def shapeZip[B, C](bs: Seq[B])
                      (implicit shapeZip: ShapeZip[A, B, C]): Seq[C] =
      shapeZip.shapeZip(as, bs)
  }

}

trait ShapeZip[A, B, C] {
  def shapeZip(as: Seq[A], b: Seq[B]): Seq[C]
}

import ShapeZip.ShapeZipOps

List(1).shapeZip(List(2))
List(1).shapeZip(List((2, 3)))
List((1, 2)).shapeZip(List((3, 4)))
List(1).shapeZip(List(2).shapeZip(List(3)))

List(1).shapeZip(List(2)).shapeZip(List(3)).shapeZip(List(4)).shapeZip(List((5, 6, 7, 8)))