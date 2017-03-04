package com.github.dronegator.nlp.sample

import shapeless._
import shapeless.ops.hlist.IsHCons

/**
  * Created by cray on 3/4/17.
  */

object DForth {
  def apply[A](implicit d: DForth[A]) =
    d

  def instance[A](f: A => List[Any]): DForth[A] =
    new DForth[A] {
      override def dd(a: A): List[Any] =
        f(a)
    }


  implicit def dForthHNil: DForth[HNil] =
    instance[HNil] { a =>
      List()
    }

  implicit def dForthHConsString[A <: HList, T <: HList](implicit
                                                         isHCons: IsHCons.Aux[A, String, T],
                                                         dForthT: DForth[T]) =
    instance[A] { a =>
      isHCons.head(a) :: dForthT.dd(isHCons.tail(a))
    }

  implicit def dForthHConsInt[A <: HList, T <: HList](implicit
                                                      isHCons: IsHCons.Aux[A, Int, T],
                                                      dForthT: DForth[T]) =
    instance[A] { a =>
      val h: Int = isHCons.head(a)
      val t: List[Any] = dForthT.dd(isHCons.tail(a))

      h :: t
    }

  implicit def dForthCaseClass[A, Repr <: HList](implicit gen: Generic.Aux[A, Repr],
                                                 dForth: DForth[Repr]) =
    instance[A] { a =>
      dForth.dd(gen.to(a))
    }
}

trait DForth[A]
  extends D[A]
