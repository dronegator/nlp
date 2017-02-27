package com.github.dronegator.nlp.main

/**
  * Created by cray on 2/27/17.
  */
package object swagger {
  type JS = String

  trait SwaggerRoute {
    def swagger: JS
  }

  object SwaggerRoute {
    def apply(f: JS) =
      new SwaggerRoute {
        override def swagger: JS =
          f
      }
  }

}



