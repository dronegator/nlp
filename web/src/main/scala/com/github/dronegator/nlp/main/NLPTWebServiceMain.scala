package com.github.dronegator.nlp.main

import java.io.File

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.RouteResult.Complete
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.main.html.NLPTWebServiceHTMLTrait
import com.github.dronegator.nlp.main.phrase._
import com.github.dronegator.nlp.main.system.NLPTWebServiceSystemTrait
import com.github.dronegator.nlp.trace._
import com.github.dronegator.nlp.utils.Match._
import com.github.dronegator.nlp.utils.{CFG, Match}
import com.github.dronegator.nlp.vocabulary.{VocabularyHintImpl, VocabularyImpl}

/**
  * Created by cray on 8/17/16.
  */

object NLPTWebServiceMain
  extends App
    with MainTools
    with Concurent
    with NLPTApp
    with NLPTWebServiceSystemTrait
    with NLPTWebServicePhraseTrait
    with NLPTWebServiceHTMLTrait
    with NLPTWebServiceUITrait {

  val fileIn :: OptFile(hints) = args.toList

  lazy val cfg = CFG()

  lazy val vocabularyHint = hints.map(load(_): VocabularyImpl)
    .getOrElse {
      VocabularyHintImpl(Tokenizer.MapOfPredefs, Map())
    }
    .time { t =>
      logger.info(s"Hints have loaded in time=$t")
    }

  lazy val vocabulary: VocabularyImpl = load(new File(fileIn)).time { t =>
    logger.info(s"Vocabulary has loaded in time=$t")
  }

  lazy val route =
    routePhrase ~
      routeSystem ~
      routeHTML ~
      routeUI

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  logger.info(s"Server online at http://localhost:8080/\nPress RETURN to stop...")

  Console.readLine() // for the future transformations

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown())

  logger.info(s"Server shutdown")

}
