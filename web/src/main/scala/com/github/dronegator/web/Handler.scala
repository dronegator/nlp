package com.github.dronegator.web

import shapeless.HList

import scala.concurrent.Future

/**
  * Created by cray on 3/5/17.
  */
object HandlerHasIOP {
  type Aux[H <: Handler[_, _, _], I1, O1, P1 <: HList] = HandlerHasIOP[H] {
    type I = I1
    type O = O1
    type P = P1

  }

  def apply[H <: Handler[I1, O1, P1], I1, O1, P1 <: HList](implicit moduleHasRS: HandlerHasIOP.Aux[H, I1, O1, P1]) =
    moduleHasRS

  def instance[H <: Handler[I1, O1, P1], I1, O1, P1 <: HList](descriptionArg: String): Aux[H, I1, O1, P1] =
    new HandlerHasIOP[H] {
      type I = I1
      type O = O1
      type P = P1
      val description: String = descriptionArg
    }
}

trait HandlerHasIOP[H <: Handler[_, _, _]] {
  type I
  type O
  type P <: HList

  val description: String

  def request: Unit = {
    println(s"request: ")
  }

  def response: Unit = {
    println(s"response: ")
  }
}

trait Handler[I, O, P <: HList] {
  def hander(request: I, path: P): Future[O]
}
