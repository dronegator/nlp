package com.github.dronegator.nlp.main.session

/**
  * Created by cray on 9/18/16.
  */

import java.util.UUID

import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.dronegator.nlp.main
import com.github.dronegator.nlp.main.session.SessionManager.CreateSession
import com.github.dronegator.nlp.main.{Concurent, NLPTAppForWeb}
import com.softwaremill.macwire._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

object NLPTWebServiceSessionTrait {

}

trait NLPTWebServiceSessionTrait
  extends
    main.NLPTAppForWeb {
  this: Concurent =>

  lazy val sessionManager = SessionManager.wrap(cfg)

  private implicit val timeout: Timeout = 10 seconds

  override lazy val route: Route =
    optionalCookie("sessionId") {
      case Some(cookie) =>
        (request) => {
          (sessionManager ask CreateSession(cookie.value))
            .flatMap{
              case SessionManager.SessionName(sessionId) =>
                (setCookie(HttpCookie("sessionId", sessionId)) {
                  logger.info(s"session continues $sessionId")
                  super.route
                })(request)
            }
        }

      case None =>
        (request) => {
          (sessionManager ask CreateSession(UUID.randomUUID().toString))
            .flatMap{
              case SessionManager.SessionName(sessionId) =>
                (setCookie(HttpCookie("sessionId", sessionId)) {
                  logger.info(s"session created $sessionId")
                  request.settings
                  super.route
                })(request)
            }
        }
    }


  //  abstract override def route: Route = pathPrefix("system") { request =>
//    (version.route ~ vocabularyStat.route) (request)
//  } ~ super.route
}


