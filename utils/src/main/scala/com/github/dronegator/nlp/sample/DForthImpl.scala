package com.github.dronegator.nlp.sample

import com.github.dronegator.nlp.sample.DForthImpl.DForth
import shapeless._
import shapeless.ops.hlist.IsHCons

/**
  * Created by cray on 3/4/17.
  */

object DForthImpl {

  trait DForth

  def apply[A](implicit d: DForthImpl[A]) =
    d

  def instance[A](f: A => List[Any]): DForthImpl[A] =
    new DForthImpl[A] {
      override def doD(a: A): List[Any] =
        f(a)
    }


  implicit def dForthHNil: DForthImpl[HNil] =
    instance[HNil] { a =>
      List()
    }

  implicit def dForthHConsString[A <: HList, T <: HList](implicit
                                                         isHCons: IsHCons.Aux[A, String, T],
                                                         dForthT: DForthImpl[T]) =
    instance[A] { a =>
      isHCons.head(a) :: dForthT.doD(isHCons.tail(a))
    }

  implicit def dForthHConsInt[A <: HList, T <: HList](implicit
                                                      isHCons: IsHCons.Aux[A, Int, T],
                                                      dForthT: DForthImpl[T]) =
    instance[A] { a =>
      val h: Int = isHCons.head(a)
      val t: List[Any] = dForthT.doD(isHCons.tail(a))

      h :: t
    }

  implicit def dForthCaseClass[A, Repr <: HList](implicit gen: Generic.Aux[A, Repr],
                                                 dForth: DForthImpl[Repr]) =
    instance[A] { a =>
      dForth.doD(gen.to(a))
    }
}

trait DForthImpl[A]
  extends D[A]
    with DForth
