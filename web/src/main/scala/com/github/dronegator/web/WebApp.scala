package com.github.dronegator.web

import shapeless.{Path => _, _}

/**
  * Created by cray on 3/5/17.
  */

trait WebAppTrait[MS <: HList] {
  def module: MS

  def description: String

  def version: String

  def schema(implicit scheme: Scheme[WebAppTrait[MS]]) =
    scheme.gen(this)
}


// web/runMain com.github.dronegator.web.WebApp



