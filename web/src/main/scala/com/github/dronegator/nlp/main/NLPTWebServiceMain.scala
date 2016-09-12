package com.github.dronegator.nlp.main

import java.io.File

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.server.Directives._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{DEOP, PEnd}
import com.github.dronegator.nlp.main.html.NLPTWebServiceHTMLTrait
import com.github.dronegator.nlp.main.phrase._
import com.github.dronegator.nlp.utils.{CFG, Match}, Match._
import com.github.dronegator.nlp.vocabulary.{VocabularyHintImpl, VocabularyImpl}
import com.github.dronegator.nlp.vocabulary.VocabularyTools.VocabularyTools

/**
 * Created by cray on 8/17/16.
 */

object NLPTWebServiceMain
  extends App
  with MainTools
  with Concurent
  with NLPTWebServiceHTMLTrait
  with NLPTWebServicePhraseTrait {

  val fileIn :: OptFile(hints) = args.toList

  lazy val cfg = CFG()

  lazy val vocabularyHint = hints.map(load(_): VocabularyImpl).getOrElse {
    println("Hints have initialized")
    VocabularyHintImpl(Tokenizer.MapOfPredefs, Map())
  }

  lazy val vocabulary: VocabularyImpl = load(new File(fileIn))


  val route =
    pathPrefix("phrase") {
      continue.route ~ suggestForNext.route ~ suggestForTheSame.route ~ suggest.route
    } ~
      routeHTML

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  Console.readLine() // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown())

  // and shutdown when done

}
