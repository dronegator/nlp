package com.github.dronegator.nlp.main.session

/**
  * Created by cray on 9/18/16.
  */

import java.util.UUID

import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.github.dronegator.nlp.main
import com.github.dronegator.nlp.main.Concurent
import com.github.dronegator.nlp.main.session.Session.SessionMessage
import com.github.dronegator.nlp.main.session.SessionManager.CreateSession
import com.github.dronegator.nlp.utils.TypeActorRef

import scala.concurrent.duration._

object NLPTWebServiceSessionTrait {

}

trait NLPTWebServiceSessionTrait
  extends
    main.NLPTAppForWeb {
  this: Concurent =>

  lazy val sessionManager = SessionManager.wrap(cfg)

  def getSession(sessionId: String) =
    system.actorSelection(s"akka://default/user/$sessionId").resolveOne()
      .map {
        TypeActorRef[SessionMessage](_)
      }
      .recover {
        case th: Throwable =>
          Session.wrap(cfg, sessionId)
      }
      .map{ x =>
        SessionManager.SessionName(sessionId)
      }


  def getSessionFromManager(sessionId: String) =
    sessionManager ask CreateSession(sessionId)

  private implicit val timeout: Timeout = 10 seconds

  override lazy val route: Route =
    optionalCookie("sessionId") {
      case Some(cookie) =>
        (request) => {
          (getSession(cookie.value))
            .flatMap {
              case SessionManager.SessionName(sessionId) =>
                (setCookie(HttpCookie("sessionId", sessionId, path = Some("/"))) {
                  logger.info(s"session continues $sessionId ${request.request.uri.path}")
                  super.route
                }) (request)
            }
        }

      case None =>
        (request) => {
          (getSession(UUID.randomUUID().toString))
            .flatMap {
              case SessionManager.SessionName(sessionId) =>
                (setCookie(HttpCookie("sessionId", sessionId, path = Some("/"))) {
                  logger.info(s"session created $sessionId ${request.request.uri.path}")
                  request.settings
                  super.route
                }) (request)
            }
        }
    }
}


