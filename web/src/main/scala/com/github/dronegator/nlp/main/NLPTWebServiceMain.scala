package com.github.dronegator.nlp.main

import java.io.File

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.main.phrase._
import com.github.dronegator.nlp.main.session.NLPTWebServiceSessionTrait
import com.github.dronegator.nlp.main.system.NLPTWebServiceSystemTrait
import com.github.dronegator.nlp.main.websocket.NLPTWebServiceSocketTrait
import com.github.dronegator.nlp.trace._
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.utils.Match._
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyHintImpl, VocabularyImpl}

/**
  * Created by cray on 8/17/16.
  */

trait NLPTAppForWeb
  extends NLPTApp {
  def route: Route =
    complete {
      HttpResponse(404, entity = "Unknown resource!")
    }
}

object NLPTWebServiceMain
  extends App
    with MainTools
    with Concurent
    with NLPTAppForWeb
    with NLPTWebServiceSystemTrait
    with NLPTWebServicePhraseTrait
    with NLPTWebServiceUITrait
    with NLPTWebServiceSocketTrait
    with NLPTWebServiceSessionTrait
    /*with NLPTWebServiceHTMLTrait */ {

  val fileIn :: OptFile(hints) = args.toList

  lazy val cfg = CFG()

  lazy val vocabularyHint = hints.map(load(_): VocabularyImpl)
    .getOrElse {
      VocabularyHintImpl(Tokenizer.MapOfPredefs, Map())
    }
    .time { t =>
      logger.info(s"Hints have loaded in time=$t")
    }

  lazy val vocabulary: Vocabulary = load(new File(fileIn)).time { t =>
    logger.info(s"Vocabulary has loaded in time=$t")
  } match {
    case vocabulary: Vocabulary =>
      vocabulary

    case vocabulary =>
      vocabulary: VocabularyImpl
  }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  logger.info(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  Console.readLine() // for the future transformations

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown())

  logger.info(s"Server shutdown")

}
