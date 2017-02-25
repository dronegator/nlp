package com.github.dronegator.nlp

/**
  * Created by cray on 2/18/17.
  */

import shapeless.ops.tuple._

import scala.annotation.implicitNotFound
import scala.concurrent._

object IsTuple {
  def apply[A](implicit isTuple: IsTuple[A]) =
    isTuple

  def createIsTuple[A] =
    new IsTuple[A] {}

  implicit def isTuple1[A] =
    createIsTuple[Tuple1[A]]

  implicit def isTuple2[A, B] =
    createIsTuple[Tuple2[A, B]]

  implicit def isTuple3[A, B, C] =
    createIsTuple[Tuple3[A, B, C]]

  implicit def isTuple4[A, B, C, D] =
    createIsTuple[Tuple4[A, B, C, D]]

  implicit def isTuple5[A, B, C, D, E] =
    createIsTuple[Tuple5[A, B, C, D, E]]

  implicit def isTuple6[A, B, C, D, E, F] =
    createIsTuple[Tuple6[A, B, C, D, E, F]]

  implicit def isTuple7[A, B, C, D, E, F, G] =
    createIsTuple[Tuple7[A, B, C, D, E, F, G]]

  implicit def isTuple8[A, B, C, D, E, F, G, H] =
    createIsTuple[Tuple8[A, B, C, D, E, F, G, H]]

  implicit def isTuple9[A, B, C, D, E, F, G, H, I] =
    createIsTuple[Tuple9[A, B, C, D, E, F, G, H, I]]

  implicit def isTuple10[A, B, C, D, E, F, G, H, I, J] =
    createIsTuple[Tuple10[A, B, C, D, E, F, G, H, I, J]]

  implicit def isTuple11[A, B, C, D, E, F, G, H, I, J, K] =
    createIsTuple[Tuple11[A, B, C, D, E, F, G, H, I, J, K]]

  implicit def isTuple12[A, B, C, D, E, F, G, H, I, J, K, L] =
    createIsTuple[Tuple12[A, B, C, D, E, F, G, H, I, J, K, L]]

  implicit def isTuple13[A, B, C, D, E, F, G, H, I, J, K, L, M] =
    createIsTuple[Tuple13[A, B, C, D, E, F, G, H, I, J, K, L, M]]

  implicit def isTuple14[A, B, C, D, E, F, G, H, I, J, K, L, M, N] =
    createIsTuple[Tuple14[A, B, C, D, E, F, G, H, I, J, K, L, M, N]]

  implicit def isTuple15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O] =
    createIsTuple[Tuple15[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O]]

  implicit def isTuple16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P] =
    createIsTuple[Tuple16[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P]]

  implicit def isTuple17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R] =
    createIsTuple[Tuple17[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R]]

  implicit def isTuple18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S] =
    createIsTuple[Tuple18[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S]]

  implicit def isTuple19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S, T] =
    createIsTuple[Tuple19[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S, T]]

  implicit def isTuple20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S, T, U] =
    createIsTuple[Tuple20[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S, T, U]]

  implicit def isTuple21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S, T, U, V] =
    createIsTuple[Tuple21[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S, T, U, V]]

  implicit def isTuple22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S, T, U, V, W] =
    createIsTuple[Tuple22[A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, R, S, T, U, V, W]]

}

trait IsTuple[A] {

}

trait FutureFlatZipLow {

  def apply[A, B, C](implicit futureFlatZip: FutureFlatZip.Aux[A, B, C]) =
    futureFlatZip

  def createFutureFlatZip[A, B, C](f: (Future[A], Future[B], ExecutionContext) => Future[C]) =
    new FutureFlatZip[A, B] {
      type Out = C

      override def futureFlatZip(as: Future[A], bs: Future[B])(implicit ec: ExecutionContext): Future[C] =
        f(as, bs, ec)
    }

  implicit def futureFlatZipAnyAny[A, B](implicit
                                         aIsNotATuple: Not[IsTuple[A]],
                                         bIsNotATuple: Not[IsTuple[B]]
                                        ) =
    createFutureFlatZip[A, B, (A, B)] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs)
    }

  implicit def futureFlatZipTupleAny[A, B, C](implicit
                                              aIsNotATuple: Not[IsTuple[A]],
                                              isTupleB: IsTuple[B],
                                              prepend: Prepend.Aux[Tuple1[A], B, C]) =
    createFutureFlatZip[A, B, C] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs).map {
        case (a, b) =>
          prepend(Tuple1(a), b)
      }
    }

  implicit def futureFlatZipAnyTuple[A, B, C](implicit
                                              isTupleA: IsTuple[A],
                                              bIsNotATuple: Not[IsTuple[B]],
                                              prepend: Prepend.Aux[A, Tuple1[B], C]) =
    createFutureFlatZip[A, B, C] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs).map {
        case (a, b) =>
          prepend(a, Tuple1(b))
      }
    }


  implicit def futureFlatZipTupleTuple[A, B, C](implicit
                                                isTupleA: IsTuple[A],
                                                isTupleB: IsTuple[B],
                                                concat: Prepend.Aux[A, B, C]) =
    createFutureFlatZip[A, B, C] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs).map {
        case (a, b) =>
          concat(a, b)
      }
    }

}


object FutureFlatZip
  extends FutureFlatZipLow {

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
  //import com.github.dronegator.nlp.FutureFlatZip
  import FutureFlatZip.FutureFlatZipOps

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent._
  import scala.concurrent.duration.Duration

  def await[A](future: Future[A]) =
    Await.result(future, Duration.Inf)

  await(Future(1).flatZip(Future(2))
    .map {
      case (x, y) =>
        x + y
    })

  await(Future(1).flatZip(Future(2).flatZip(Future(3)))
    .map {
      case (x, y, z) =>
        x + y + z
    })

  await(Future(1).flatZip((Future(2).flatZip(Future(3))))
    .map {
      case (x, y, z) =>
        x + y + z
    })

  await(Future(1).flatZip(Future(2)).flatZip((Future(3).flatZip(Future(4))))
    .map {
      case (x, y, z, z1) =>
        x + y + z + z1
    })


  val a1 = await(Future(Seq(1)).flatZip(Future(Seq(2))).flatZip(Future(Seq(2))).flatZip(Future(Seq(4)))
    .map {
      case (a, b, c, d) =>
        a ++ b ++ c ++ d
    })

  println(a1)

}

//object Example {
//
//  import FutureFlatZip.FutureFlatZipOps
//
//  import scala.concurrent.ExecutionContext.Implicits.global
//
//  val qq = the[FutureFlatZip[Int, (Int, Int)]]
//
//  qq.futureFlatZip(Future(1), Future((2, 3))).map {
//    case (x, y, z) =>
//      x + y + z
//  }
//
//  Future(1).flatZip(Future(2))
//
//  Future(1).flatZip(Future((2, 3))).map {
//    case (x, y, z) =>
//      x + y + z
//  }
//  Future((1, 2)).flatZip(Future((3, 4)))
//  Future(1).flatZip(Future(2).flatZip(Future(3)))
//
//  Future(1).flatZip(Future(2)).flatZip(Future(3)).flatZip(Future(4)).flatZip(Future((5, 6, 7, 8)))
//}
