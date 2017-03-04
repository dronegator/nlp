package com.github.dronegator.nlp.sample

import com.github.dronegator.nlp.sample.DBackImpl.DBack
import shapeless._
import shapeless.ops.hlist.IsHCons

/**
  * Created by cray on 3/4/17.
  */

object DBackImpl {

  trait DBack

  def apply[A](implicit d: DBackImpl[A]) =
    d

  def instance[A](f: A => List[Any]): DBackImpl[A] =
    new DBackImpl[A] {
      override def doD(a: A): List[Any] =
        f(a)
    }


  implicit def dBackHNil: DBackImpl[HNil] =
    instance[HNil] { a =>
      List()
    }

  implicit def dBackHConsString[A <: HList, T <: HList](implicit
                                                        isHCons: IsHCons.Aux[A, String, T],
                                                        dBackT: DBackImpl[T]) =
    instance[A] { a =>
      dBackT.doD(isHCons.tail(a)) :+ isHCons.head(a)
    }

  implicit def dBackHConsInt[A <: HList, T <: HList](implicit
                                                     isHCons: IsHCons.Aux[A, Int, T],
                                                     dBackT: DBackImpl[T]) =
    instance[A] { a =>
      val h: Int = isHCons.head(a)
      val t: List[Any] = dBackT.doD(isHCons.tail(a))

      t :+ h
    }

  implicit def dBackCaseClass[A, Repr <: HList](implicit gen: Generic.Aux[A, Repr],
                                                dBack: DBackImpl[Repr]) =
    instance[A] { a =>
      dBack.doD(gen.to(a))
    }
}

trait DBackImpl[A]
  extends D[A]
    with DBack
