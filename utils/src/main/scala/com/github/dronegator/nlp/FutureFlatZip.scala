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
}

trait IsTuple[A] {

}

sealed trait Existence

trait Exists extends Existence

trait NotExists extends Existence

trait IsTypeClassExists[TypeClass, Answer]

object IsTypeClassExists {
  private val evidence: IsTypeClassExists[Any, Any] =
    new Object with IsTypeClassExists[Any, Any]

  implicit def typeClassExistsEv[TypeClass, Answer](implicit a: TypeClass) =
    evidence.asInstanceOf[IsTypeClassExists[TypeClass, Exists]]

  implicit def typeClassNotExistsEv[TypeClass, Answer] =
    evidence.asInstanceOf[IsTypeClassExists[TypeClass, NotExists]]
}

@implicitNotFound("Argument does not satisfy constraints: Not ${T}")
trait Not[T]

object Not {
  private val evidence: Not[Any] = new Object with Not[Any]

  implicit def notEv[T, Answer](implicit a: IsTypeClassExists[T, Answer], ne: Answer =:= NotExists) =
    evidence.asInstanceOf[Not[T]]
}

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
                                         aIsNotATuple: Not[IsTuple[A]],
                                         bIsNotATuple: Not[IsTuple[B]]
                                        ) =
    createFutureFlatZip[A, B, (A, B)] { (as, bs, ec) =>
      implicit val iec = ec
      as.zip(bs)
    }

}

trait FutureFlatZipLow
  extends FutureFlatZipLowest {
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

}

trait FutureFlatZipHigh
  extends FutureFlatZipLow {
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
