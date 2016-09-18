package com.github.dronegator.nlp.main.html

import com.github.dronegator.nlp.main.{NLPTApp, Concurent}

import akka.http.scaladsl.marshallers.xml.ScalaXmlSupport._
import akka.http.scaladsl.server.Directives._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{DEOP, PEnd}
import com.github.dronegator.nlp.vocabulary.VocabularyTools.VocabularyTools

/**
  * Created by cray on 9/12/16.
  */
object NLPTWebServiceHTMLTrait {

}

trait NLPTWebServiceHTMLTrait {
  this: NLPTApp with Concurent =>

  lazy val routeHTML = path("generate") {
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
    path("continue1") {
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
}


