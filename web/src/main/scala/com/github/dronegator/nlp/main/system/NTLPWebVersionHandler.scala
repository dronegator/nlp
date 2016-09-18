package com.github.dronegator.nlp.main.system

/**
  * Created by cray on 9/17/16.
  */
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.dronegator.nlp.main.Handler.RequestEmpty
import com.github.dronegator.nlp.main.{Handler, Version}
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by cray on 9/15/16.
  */

object NTLPWebSystemVersionHandler extends DefaultJsonProtocol {

  //case class Request()

  case class ResponseVersion(name: String,
                             version: String,
                             branch: String,
                             commit: String,
                             buildTime: String)

  implicit val suggestPhraseFormat = jsonFormat5(ResponseVersion)
}

class NTLPWebSystemVersionHandler()(implicit context: ExecutionContext)
  extends Handler[RequestEmpty, NTLPWebSystemVersionHandler.ResponseVersion] {


  import NTLPWebSystemVersionHandler._

  def route: Route =
    path("version") {
      get {
        complete {
          handle(RequestEmpty())
        }
      }
    }

  override def handle(request: RequestEmpty): Future[NTLPWebSystemVersionHandler.ResponseVersion] = Future {
    ResponseVersion(
      name = Version.name,
      version = Version.version,
      branch = Version.branch,
      commit = Version.commit,
      buildTime = Version.buildTime
    )
  }
}

