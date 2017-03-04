package com.github.dronegator.nlp.sample

import shapeless._
import shapeless.ops.hlist.IsHCons

/**
  * Created by cray on 3/4/17.
  */

object DBack {
  def apply[A](implicit d: DBack[A]) =
    d

  def instance[A](f: A => List[Any]): DBack[A] =
    new DBack[A] {
      override def dd(a: A): List[Any] =
        f(a)
    }


  implicit def dBackHNil: DBack[HNil] =
    instance[HNil] { a =>
      List()
    }

  implicit def dBackHConsString[A <: HList, T <: HList](implicit
                                                        isHCons: IsHCons.Aux[A, String, T],
                                                        dBackT: DBack[T]) =
    instance[A] { a =>
      dBackT.dd(isHCons.tail(a)) :+ isHCons.head(a)
    }

  implicit def dBackHConsInt[A <: HList, T <: HList](implicit
                                                     isHCons: IsHCons.Aux[A, Int, T],
                                                     dBackT: DBack[T]) =
    instance[A] { a =>
      val h: Int = isHCons.head(a)
      val t: List[Any] = dBackT.dd(isHCons.tail(a))

      t :+ h
    }

  implicit def dBackCaseClass[A, Repr <: HList](implicit gen: Generic.Aux[A, Repr],
                                                dBack: DBack[Repr]) =
    instance[A] { a =>
      dBack.dd(gen.to(a))
    }
}

trait DBack[A]
  extends D[A]
