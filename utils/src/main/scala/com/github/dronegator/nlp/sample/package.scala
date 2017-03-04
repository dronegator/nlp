package com.github.dronegator.nlp

/**
  * Created by cray on 3/4/17.
  */
package object sample {

  class QD {
    def qd[A, D1 <: D[A]](a: A)(implicit q: Q[A, D1]) =
      q.qq(a)
  }

  trait D[A] {
    def dd(a: A): List[Any]
  }


  object Q {
    implicit def q[A, D1 <: D[A]](implicit d: D1) =
      new Q[A, D1]

  }

  class Q[A, D1 <: D[A]](implicit d: D1) {
    def qq(a: A): List[Any] =
      d.dd(a)
  }
}
