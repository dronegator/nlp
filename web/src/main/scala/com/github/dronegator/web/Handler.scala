package com.github.dronegator.web

import shapeless.HList

import scala.concurrent.Future

/**
  * Created by cray on 3/5/17.
  */
trait Handler[I, O, P <: HList] {
  def hander(request: I, path: P): Future[O]
}
