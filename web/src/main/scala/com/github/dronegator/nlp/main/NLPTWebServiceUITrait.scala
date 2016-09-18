package com.github.dronegator.nlp.main

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/**
  * Created by cray on 9/18/16.
  */
trait NLPTWebServiceUITrait
  extends NLPTAppForWeb {
  this: Concurent  =>

  abstract override def route: Route = path("ui") {
    getFromResource("ui/index.html")
  } ~
    pathPrefix("ui/js") {
      getFromResourceDirectory("ui/js")
    } ~
    pathPrefix("ui") {
      getFromResourceDirectory("ui")
    } ~ super.route
}
