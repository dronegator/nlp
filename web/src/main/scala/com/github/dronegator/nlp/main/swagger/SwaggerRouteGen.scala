package com.github.dronegator.nlp.main.swagger

/**
  * Created by cray on 2/27/17.
  */

object SwaggerRouteGen {
  def apply[R <: SwaggerRoute](implicit swaggerRouteGen: SwaggerRouteGen[R]) =
    swaggerRouteGen

  def createSwaggerRoute[R <: SwaggerRoute](f: => JS) =
    new SwaggerRouteGen[R] {
      override def swagger: JS =
        f
    }

  implicit def swaggerRouteGen[R <: SwaggerRoute](implicit swaggerRoute: R) =
    createSwaggerRoute[R] {
      swaggerRoute.swagger
    }
}

trait SwaggerRouteGen[R <: SwaggerRoute] {
  def swagger: JS
}
