package com.github.dronegator.web

import shapeless.HList

/**
  * Created by cray on 3/5/17.
  */
trait Route[A <: HList] {
  val path: String //TODO: Just  for awhile

}
