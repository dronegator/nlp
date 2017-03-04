package com.github.dronegator.nlp

/**
  * Created by cray on 3/4/17.
  */
package object sample {

  class QD {
    def qd[A, T](a: A)(implicit q: Q[A, D[A] with T]) =
      q.qq(a)
  }

  trait D[A] {
    def dd(a: A): List[Any]
  }


  object Q {
    implicit def q[A, T](implicit d: D[A] with T) =
      new Q[A, D[A] with T]

  }

  class Q[A, T](implicit d: D[A] with T) {
    def qq(a: A): List[Any] =
      d.dd(a)
  }
}
