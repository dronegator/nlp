package com.github.dronegator.web

import akka.stream.Materializer
import spray.json.JsValue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by cray on 3/25/17.
  */

trait ResponseSerializer[R] {
  def serialize(r: R)
               (implicit ec: ExecutionContext, mat: Materializer): Future[JsValue]
}

import spray.json._

object ResponseSerializer {
  def apply[R](implicit responseSerializer: ResponseSerializer[R]) =
    responseSerializer


  def createResponseSerializer[R](f: (R, ExecutionContext, Materializer) => Future[JsValue]) =
    new ResponseSerializer[R] {
      override def serialize(r: R)
                            (implicit ec: ExecutionContext, mat: Materializer): Future[JsValue] =
        f(r, ec, mat)
    }

  implicit def responseSerializerR[R](implicit jsonWriter: JsonWriter[R]) =
    createResponseSerializer[R] { (r: R, ec: ExecutionContext, mat: Materializer) =>
      implicit val implicitEc = ec
      implicit val implicitMat = mat

      Future(r.toJson)
    }
}
