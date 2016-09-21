package com.github.dronegator.nlp.main.session

/**
 * Created by cray on 9/18/16.
 */

import java.util.UUID

import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.github.dronegator.nlp.main.session.SessionManager.CreateSession
import com.github.dronegator.nlp.main.session.SessionStorage.SessionMessage
import com.github.dronegator.nlp.main.{Concurent, NLPTAppForWeb}
import com.github.dronegator.nlp.utils.typeactor._

import scala.concurrent.duration._

object NLPTWebServiceSessionTrait {

}

protected trait SessionStorageTrait {
  this: NLPTAppForWeb with Concurent =>

  lazy val propsSessionStorage = SessionStorage.props(cfg)

  private implicit val timeout: Timeout = 10 seconds

  def getSession(sessionId: SessionId) =
    system.actorSelection(s"akka://default/user/$sessionId")
      .resolveOne()
      .map {
        TypeActorRef[SessionMessage](_)
      }
      .recover {
        case th: Throwable =>
          system.actorOf(propsSessionStorage)
      }
      .map { typeActorRef =>
        SessionManager.SessionRef(sessionId, typeActorRef)
      }
}

protected trait SessionManagerTrait {
  this: NLPTAppForWeb with Concurent with SessionStorageTrait =>

  lazy val propsSessionManager = SessionManager.props(cfg, propsSessionStorage)

  lazy val sessionManager = system.actorOf(propsSessionManager)

  private implicit val timeout: Timeout = 10 seconds

  override def getSession(sessionId: SessionId) =
    (sessionManager ask CreateSession(SessionId(sessionId))).mapTo[SessionManager.SessionRef]

}

trait NLPTWebServiceSessionTrait
  extends NLPTAppForWeb
  with SessionStorageTrait
  /*with SessionManagerTrait */ {
  this: Concurent =>

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
