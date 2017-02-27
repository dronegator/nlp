package com.github.dronegator.nlp.main.swagger

import shapeless._

/**
  * Created by cray on 2/27/17.
  */
object SwaggerExampleApp
  extends App {

  implicit object Q1 extends SwaggerRoute {
    override def swagger: JS = " q1 "
  }

  object Q2 extends SwaggerRoute {
    override def swagger: JS = " q2 "
  }

  implicit def q2Gen = SwaggerRouteGen.swaggerRouteGen(Q2)

  val swaggerSpec = SwaggerAppGen[Q1.type :: Q2.type :: HNil]

  println(swaggerSpec.swagger)
}
