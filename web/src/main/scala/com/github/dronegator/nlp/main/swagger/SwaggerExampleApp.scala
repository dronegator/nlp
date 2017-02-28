package com.github.dronegator.nlp.main.swagger

import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Word
import com.github.dronegator.nlp.main.phrase.{AdviceHandler, ContinueHandler, Request, Response}
import shapeless._

/**
  * Created by cray on 2/27/17.
  */
object SwaggerExampleApp
  extends App {

  implicit object Q1 extends SwaggerRoute[AdviceHandler] {
    override def swagger: JS = " q1 "
  }

  implicit object Q2 extends SwaggerRoute[ContinueHandler] {
    override def swagger: JS = " q2 "
  }

  implicit def q2Gen = SwaggerRouteGen.swaggerRouteGen[Request[ContinueHandler.Data], Response[Word], ContinueHandler, SwaggerRoute[ContinueHandler]](Q2)

  //implicit val a = implicitly[SwaggerRouteGen[Request[ContinueHandler.Data], Response[Word], ContinueHandler, SwaggerRoute[ContinueHandler]]]

  //implicit val b = implicitly[IsHCons.Aux[SwaggerRoute[ContinueHandler] :: HNil, SwaggerRoute[ContinueHandler], HNil]]

  //implicit val sSpec = SwaggerAppGen[HNil]

  //implicit val swaggerSpec_ = SwaggerAppGen.swaggerAppGenHCons[SwaggerRoute[ContinueHandler] :: HNil, Request[ContinueHandler.Data], Response[Word], ContinueHandler, SwaggerRoute[ContinueHandler], HNil]

  val swaggerSpec = SwaggerAppGen[Q1.type :: HNil]

  println(swaggerSpec.swagger)
}
