package com.github.dronegator.nlp.main.session

/**
  * Created by cray on 9/18/16.
  */

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.github.dronegator.nlp.main
import com.github.dronegator.nlp.main.Concurent
import com.github.dronegator.nlp.main.session.SessionManager.CreateSession
import com.github.dronegator.nlp.main.session.SessionStorage.SessionMessage
import com.github.dronegator.nlp.utils.TypeActorRef
import com.softwaremill.tagging.{@@, _}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

object NLPTWebServiceSessionTrait {


}

trait NLPTWebServiceSessionTrait
  extends
    main.NLPTAppForWeb {
  this: Concurent =>

  import NLPTWebServiceSessionTrait._

  lazy val sessionManager = SessionManager.wrap(cfg)

  def getSession(sessionId: SessionId) =
    system.actorSelection(s"akka://default/user/$sessionId")
      .resolveOne()
      .map {
        TypeActorRef[SessionMessage](_)
      }
      .recover {
        case th: Throwable =>
          SessionStorage.wrap(cfg, sessionId)
      }
      .map { typeActorRef =>
        SessionManager.SessionRef(sessionId, typeActorRef)
      }


  def getSessionFromManager(sessionId: String) =
    sessionManager ask CreateSession(SessionId(sessionId))

  private implicit val timeout: Timeout = 10 seconds

  override lazy val route: Route =
    optionalCookie("sessionId") {
      case Some(cookie) =>
        (request) => {
          (getSession(SessionId(cookie.value)))
            .flatMap {
              case SessionManager.SessionRef(sessionId, typeActorRef) =>
                (setCookie(HttpCookie("sessionId", sessionId, path = Some("/"))) {
                  logger.info(s"session continues $sessionId ${request.request.uri.path}")
                  super.route
                }) (request)
            }
        }

      case None =>
        (request) => {
          (getSession(SessionId(UUID.randomUUID().toString)))
            .flatMap {
              case SessionManager.SessionRef(sessionId, typeActorRef) =>
                (setCookie(HttpCookie("sessionId", sessionId, path = Some("/"))) {
                  logger.info(s"session created $sessionId ${request.request.uri.path}")
                  request.settings
                  super.route
                }) (request)
            }
        }
    }
}


