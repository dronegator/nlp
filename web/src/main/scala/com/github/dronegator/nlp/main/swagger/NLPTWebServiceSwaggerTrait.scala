package com.github.dronegator.nlp.main.swagger

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.dronegator.nlp.main.{Concurent, NLPTAppForWeb}

/**
  * Created by cray on 2/26/17.
  */
trait NLPTWebServiceSwaggerTrait
  extends NLPTAppForWeb {
  this: Concurent =>

  abstract override def route: Route = pathPrefix("swagger-ui") {
    getFromResourceDirectory("swagger-ui")
  } ~
    pathPrefix("ui") {
      getFromResourceDirectory("ui")
    } ~ super.route
}
