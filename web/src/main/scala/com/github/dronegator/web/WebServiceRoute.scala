package com.github.dronegator.web

import akka.http.scaladsl.server._
import shapeless.HList

/**
  * Created by cray on 3/20/17.
  */

trait WebServiceRoute[MS <: HList]
  extends WebAppTrait[MS] {

  def webServiceRoute(implicit genRoute: GenRoute[MS]): Route =
    genRoute.gen(module)
}
