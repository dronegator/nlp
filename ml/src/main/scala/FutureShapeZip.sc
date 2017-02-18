import shapeless._
import shapeless.ops.hlist._
import scala.concurrent._

trait ShapeZipLowest {

  def apply[A, B, C](implicit shapeZip: ShapeZip[A, B, C]) =
    shapeZip

  def createShapeZip[A, B, C](f: (Future[A], Future[B], ExecutionContext) => Future[C]) =
    new ShapeZip[A, B, C] {
      override def shapeZip(as: Future[A], bs: Future[B])(implicit ec: ExecutionContext): Future[C] =
        f(as, bs, ec)
    }

  implicit def shapeZipAnyAny[A, B](implicit
                                    aNotAProduct: A <:!< Product,
                                    bNotAProduct: B <:!< Product) =
    createShapeZip[A, B, (A, B)] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs)
    }

  implicit def shapeZipAnyTuple[A, B, BRepl <: HList, C](implicit
                                                         aNotAProduct: A <:!< Product,
                                                         genB: Generic.Aux[B, BRepl],
                                                         tupler: Tupler.Aux[A :: BRepl, C]) =
    createShapeZip[A, B, C] { (as, bs, ec) =>
      implicit val iec = ec
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
    createShapeZip[A, B, C] { (as, bs, ec) =>
      implicit val iec = ec
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
    createShapeZip[A, B, C] { (as, bs, ec) =>
      implicit val iec = ec
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

  implicit class ShapeZipOps[A](as: Future[A]) {
    def shapeZip[B, C](bs: Future[B])
                      (implicit
                       shapeZip: ShapeZip[A, B, C],
                       ec: ExecutionContext): Future[C] =
      shapeZip.shapeZip(as, bs)
  }

}

trait ShapeZip[A, B, C] {
  def shapeZip(as: Future[A], b: Future[B])(implicit ec: ExecutionContext): Future[C]
}

import ShapeZip.ShapeZipOps

import scala.concurrent.ExecutionContext.Implicits.global

Future(1).shapeZip(Future(2))
Future(1).shapeZip(Future((2, 3)))
Future((1, 2)).shapeZip(Future((3, 4)))
Future(1).shapeZip(Future(2).shapeZip(Future(3)))

Future(1).shapeZip(Future(2)).shapeZip(Future(3)).shapeZip(Future(4)).shapeZip(Future((5, 6, 7, 8)))