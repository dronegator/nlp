package com.github.dronegator.nlp

/**
  * Created by cray on 3/4/17.
  */
package object sample {

  object TD {
    implicit def tqd[T] =
      new TD[T] {

      }
  }

  trait TD[T] {
    def doD[A](a: A)
              (implicit d: D[A] with T) =
      d.doD(a)
  }

  trait D[A] {
    def doD(a: A): List[Any]
  }
}
