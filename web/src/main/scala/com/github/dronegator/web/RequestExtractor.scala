package com.github.dronegator.web

import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server._
import akka.http.scaladsl.unmarshalling.{Unmarshal, Unmarshaller}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by cray on 3/25/17.
  */

trait RequestExtractor[R] {
  def extract(ctx: RequestContext)
             (implicit ec: ExecutionContext, mat: Materializer): Future[R]
}

object RequestExtractor {
  def apply[R](implicit requestExtractor: RequestExtractor[R]) =
    requestExtractor


  def createRequestExtractor[R](f: (RequestContext, ExecutionContext, Materializer) => Future[R]) =
    new RequestExtractor[R] {
      override def extract(x: RequestContext)
                          (implicit ec: ExecutionContext, mat: Materializer): Future[R] =
        f(x, ec, mat)
    }

  implicit def requestExtractorR[R](implicit um: Unmarshaller[HttpRequest, R]) =
    createRequestExtractor[R] { (ctx: RequestContext, ec: ExecutionContext, mat: Materializer) =>
      Unmarshal(ctx.request).to[R](um, ec, mat)
    }
}

