package com.github.dronegator.nlp.main

import java.io.File

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.server.Directives._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{DEOP, PEnd}
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
  with DumpTools {

  val fileIn :: OptFile(hints) = args.toList
  lazy val cfg = CFG()

  lazy val vocabularyHint = hints.map(load(_): VocabularyImpl).getOrElse{
    println("Hints have initialized")
    VocabularyHintImpl(Tokenizer.MapOfPredefs, Map())
  }

  lazy val vocabulary: VocabularyImpl = load(new File(fileIn))

  val route =
    path("generate") {
      get {
        {
          parameter("words".as[String]) {
            phrase =>
              complete {
                <div>
                  <h1>Generate phrase from:
                    {phrase}
                  </h1>{vocabulary.generatePhrase(vocabulary.tokenizeShort(phrase)).
                  map {
                    case tokens =>
                      <p>
                        {vocabulary.untokenize(tokens)}
                      </p>
                  } toList}
                </div>
              }
          }
        }
      }
    } ~
      path("advice") {
        get {
          {
            parameter("words".as[String]) {
              phrase =>
                complete {


                  <div>
                    <h1>Advice improvements for:
                      {phrase}
                    </h1>
                    <table>x
                      {vocabulary.advicePlain(vocabulary.tokenize(phrase)).
                      map {
                        case (statements, n) if !statements.isEmpty =>
                          statements.map {
                            case (statement, d) =>
                              val phrase = statement.flatMap(vocabulary.wordMap.get(_)).mkString(" ")
                              <tr>
                                <td>
                                  {f"$d%5.4f"}
                                </td> <td>
                                {phrase}
                              </td>
                              </tr>
                          }

                        case _ =>
                          <p></p>
                      }.toList}
                    </table>
                  </div>
                }
            }
          }
        }
      } ~
      path("continue") {
        get {
          {
            parameter("words".as[String]) {
              phrase => complete {
                vocabulary.tokenize(phrase) match {
                  case tokens@(_ :+ DEOP.value :+ PEnd.value) =>
                    <div>
                      <h1>
                        We suggest a few words for the next phrase after
                        {phrase}
                        :
                      </h1>
                      <table>
                        {vocabulary.suggestForNext(tokens).
                        flatMap {
                          case (token, p) =>
                            vocabulary.wordMap.get(token).map(_ -> p)
                        }.
                        map {
                          case (word, p) =>
                            <tr>
                              <td>
                                {word}
                              </td> <td>
                              {p}
                            </td>
                            </tr>
                        }}
                      </table>
                    </div>
                  case tokens =>
                    <div>
                      <h1>Continue phrase
                        {phrase}
                        with:</h1>
                      <table>
                        {vocabulary.continueStatement(vocabulary.tokenizeShort(phrase)) map {
                        case (token, probability) =>
                          <tr>
                            <td>
                              {vocabulary.wordMap.getOrElse(token, "")}
                            </td> <td>
                            {probability}
                          </td>
                          </tr>
                      }}
                      </table>
                    </div>

                }
              }
            }
          }
        }
      }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  Console.readLine() // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown())

  // and shutdown when done

  override def tokenToDump(token: Token): String = ???
}
