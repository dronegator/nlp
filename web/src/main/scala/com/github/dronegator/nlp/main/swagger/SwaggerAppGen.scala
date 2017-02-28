package com.github.dronegator.nlp.main.swagger

import com.github.dronegator.nlp.main.Handler
import shapeless.ops.hlist.IsHCons
import shapeless.{HList, HNil}

/**
  * Created by cray on 2/27/17.
  */
object SwaggerAppGen {

  def apply[HS <: HList](implicit sAppGen: SwaggerAppGen[HS]) =
    sAppGen


  def createSwaggerAppGen[HS <: HList](f: => JS) =
    new SwaggerAppGen[HS] {
      override def swagger: JS =
        f
    }

  implicit def swaggerAppGenHNil =
    createSwaggerAppGen[HNil]("")

  implicit def swaggerAppGenHCons[HS <: HList, I, O, H <: Handler[I, O], R <: SwaggerRoute[H], T <: HList](implicit isHCons: IsHCons.Aux[HS, R, T],
                                                                                                           swaggerRouteGen: SwaggerRouteGen[I, O, H, R],
                                                                                                           swaggerAppGen: SwaggerAppGen[T]) =
    createSwaggerAppGen[HS] {
      swaggerRouteGen.swagger ++ swaggerAppGen.swagger
    }
}

sealed trait SwaggerAppGen[HS <: HList] {
  def swagger: JS
}
