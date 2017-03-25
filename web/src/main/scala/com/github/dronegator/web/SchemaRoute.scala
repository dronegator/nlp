package com.github.dronegator.web

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.github.dronegator.web.AnyJsonFormat.Format
import shapeless.{Path => _, _}
import spray.json._

import scala.concurrent.Future

/**
  * Created by cray on 3/20/17.
  */
trait SchemaRoute[MS <: HList]
  extends WebAppTrait[MS]
    with DefaultJsonProtocol {


  def schemaRoute(implicit scheme: Scheme[WebAppTrait[MS]]): Route =

  //    path("swagger-ui" / "swagger.json") {
  //      getFromResource("swagger-ui/swaggerExampleApp.json")
  //    } ~
    path("swagger-ui" / "swagger.json") { ctx =>
      ctx.complete(Future.successful(schema.toJson))
    } ~
      pathPrefix("swagger-ui") {
        getFromResourceDirectory("swagger-ui")
      } ~
      pathPrefix("ui") {
        getFromResourceDirectory("ui")
      }

}
