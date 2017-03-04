package com.github.dronegator.nlp

/**
  * Created by cray on 3/4/17.
  */
package object sample {

  class QDForTag[T](originalQd: QD) {
    def qd[A](a: A)(implicit q: Q[A, D[A] with T]) =
      originalQd.qd[A, T](a)

    def forTag[T1] = new QDForTag[T](originalQd)
  }


  object qd {
    implicit def tqd[T] =
      new qd[T] {

      }
  }

  trait qd[T] {
    def qd[A](a: A)
             (implicit d: D[A] with T) =
      d.dd(a)
  }

  class QD {
    def qd[A, T](a: A)
                (implicit q: Q[A, D[A] with T]) =
      q.qq(a)

    def tqd[T](implicit tqd: qd[T]): qd[T] =
      tqd

    def forTag[T] = new QDForTag[T](this)
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
