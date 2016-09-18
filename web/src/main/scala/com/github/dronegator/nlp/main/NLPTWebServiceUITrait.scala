package com.github.dronegator.nlp.main

import akka.http.scaladsl.server.Directives._

/**
  * Created by cray on 9/18/16.
  */
trait NLPTWebServiceUITrait {
  this: NLPTApp with Concurent  =>

  lazy val routeUI = path("ui") {
    getFromResource("ui/index.html")
  } ~
    pathPrefix("ui/js") {
      getFromResourceDirectory("ui/js")
    } ~
    pathPrefix("ui") {
      getFromResourceDirectory("ui")
    }
}
