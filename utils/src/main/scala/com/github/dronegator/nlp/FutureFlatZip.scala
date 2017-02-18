package com.github.dronegator.nlp

/**
  * Created by cray on 2/18/17.
  */

import shapeless._
import shapeless.ops.hlist._
import scala.concurrent._

trait FutureFlatZipLowest {

  def apply[A, B, C](implicit futureFlatZip: FutureFlatZip.Aux[A, B, C]) =
    futureFlatZip

  def createFutureFlatZip[A, B, C](f: (Future[A], Future[B], ExecutionContext) => Future[C]) =
    new FutureFlatZip[A, B] {
      type Out = C
      override def futureFlatZip(as: Future[A], bs: Future[B])(implicit ec: ExecutionContext): Future[C] =
        f(as, bs, ec)
    }

  implicit def futureFlatZipAnyAny[A, B](implicit
                                         aNotAProduct: A <:!< Product,
                                         bNotAProduct: B <:!< Product) =
    createFutureFlatZip[A, B, (A, B)] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs)
    }

  implicit def futureFlatZipAnyTuple[A, B, BRepl <: HList, C](implicit
                                                              aNotAProduct: A <:!< Product,
                                                              genB: Generic.Aux[B, BRepl],
                                                              tupler: Tupler.Aux[A :: BRepl, C]) =
    createFutureFlatZip[A, B, C] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs).map {
        case (a, b) =>
          tupler(a :: genB.to(b))
      }
    }

  implicit def futureFlatZipTupleAny[A, ARepl <: HList, B, C, CRepl <: HList](implicit
                                                                              bNotAProduct: B <:!< Product,
                                                                              genA: Generic.Aux[A, ARepl],
                                                                              prepend: Prepend.Aux[ARepl, B :: HNil, CRepl],
                                                                              tupler: Tupler.Aux[CRepl, C]) =
    createFutureFlatZip[A, B, C] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs).map {
        case (a, b) =>
          tupler(genA.to(a) :+ b)
      }
    }

  implicit def futureFlatZipTupleTuple[A, ARepl <: HList, B, BRepl <: HList, C, CRepl <: HList](implicit
                                                                                                genA: Generic.Aux[A, ARepl],
                                                                                                genB: Generic.Aux[B, BRepl],
                                                                                                concat: Prepend.Aux[ARepl, BRepl, CRepl],
                                                                                                tupler: Tupler.Aux[CRepl, C]) =
    createFutureFlatZip[A, B, C] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs).map {
        case (a, b) =>
          tupler(genA.to(a) ++ genB.to(b))
      }
    }

}

trait FutureFlatZipHigh
  extends FutureFlatZipLowest {
}


object FutureFlatZip
  extends FutureFlatZipHigh {

  type Aux[A, B, C] = FutureFlatZip[A, B] {
    type Out = C
  }

  implicit class FutureFlatZipOps[A](as: Future[A]) {
    def flatZip[B, C](bs: Future[B])
                     (implicit
                      futureFlatZip: FutureFlatZip.Aux[A, B, C],
                      ec: ExecutionContext): Future[C] =
      futureFlatZip.futureFlatZip(as, bs)
  }

}

trait FutureFlatZip[A, B] {
  type Out

  def futureFlatZip(as: Future[A], b: Future[B])(implicit ec: ExecutionContext): Future[Out]
}

object Example {

  import FutureFlatZip.FutureFlatZipOps

  import scala.concurrent.ExecutionContext.Implicits.global

  val qq = the[FutureFlatZip[Int, (Int, Int)]]

  qq.futureFlatZip(Future(1), Future((2, 3))).map {
    case (x, y, z) =>
      x + y + z
  }

  Future(1).flatZip(Future(2))

  Future(1).flatZip(Future((2, 3))).map {
    case (x, y, z) =>
      x + y + z
  }
  Future((1, 2)).flatZip(Future((3, 4)))
  Future(1).flatZip(Future(2).flatZip(Future(3)))

  Future(1).flatZip(Future(2)).flatZip(Future(3)).flatZip(Future(4)).flatZip(Future((5, 6, 7, 8)))
}
