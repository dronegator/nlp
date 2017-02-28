package com.github.dronegator.nlp.main.swagger

import com.github.dronegator.nlp.main.Handler

/**
  * Created by cray on 2/27/17.
  */

object SwaggerRouteGen {
  def apply[I, O, H <: Handler[I, O], R <: SwaggerRoute[H]](implicit swaggerRouteGen: SwaggerRouteGen[I, O, H, R]) =
    swaggerRouteGen

  def createSwaggerRoute[I, O, H <: Handler[I, O], R <: SwaggerRoute[H]](f: => JS) =
    new SwaggerRouteGen[I, O, H, R] {
      override def swagger: JS =
        f
    }

  implicit def swaggerRouteGen[I, O, H <: Handler[I, O], R <: SwaggerRoute[H]](implicit swaggerRoute: R) =
    createSwaggerRoute[I, O, H, R] {
      swaggerRoute.swagger
    }
}

trait SwaggerRouteGen[I, O, H <: Handler[I, O], R <: SwaggerRoute[H]] {
  def swagger: JS
}
