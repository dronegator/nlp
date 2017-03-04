package com.github.dronegator.nlp.sample

/**
  * Created by cray on 3/4/17.
  */
class QService {
  def tag[T](implicit td: TD[T]): TD[T] =
    td

  def dBack[A](a: A)(implicit dBack: DBackImpl[A]) =
    dBack.doD(a)

  def dForth[A](a: A)(implicit dForth: DForthImpl[A]) =
    dForth.doD(a)

}
