package com.github.dronegator.nlp.main

/**
  * Created by cray on 2/27/17.
  */
package object swagger {
  type JS = String

  trait SwaggerRoute[H <: Handler[_, _]] {
    def swagger: JS
  }

  object SwaggerRoute {
    def apply[I, O, Hander](f: JS) =
      new SwaggerRoute[Handler[I, O]] {
        override def swagger: JS =
          f
      }
  }

}



