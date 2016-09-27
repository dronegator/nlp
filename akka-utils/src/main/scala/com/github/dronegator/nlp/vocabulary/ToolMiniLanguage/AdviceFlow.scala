package com.github.dronegator.nlp.vocabulary.ToolMiniLanguage

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.Vocabulary
import com.typesafe.scalalogging.LazyLogging

/**
 * Created by cray on 9/26/16.
 */
object AdviceFlow extends LazyLogging {
  type StatementId = Int

  def apply(vocabulary: Vocabulary, tokens: Set[Token]): Flow[Token, Iterator[Token], NotUsed] = {
    val statementsIn = vocabulary.statements
      .filter { x =>
        x.length > 5 && x.length < 15
      }

    val statements = calcRest(statementsIn, tokens)

    println(s"size = ${statements.size} ${statements.values.flatten.size}")

    val token2Statement =
      statementsIn
        .flatMap { statement =>
          statement
            .map {
              _ -> statement
            }

        }
        .groupBy(_._1)
        .map {
          case (x, ys) =>
            x -> ys.map(_._2)
        }

    val state =
      AdviceFlowState(token2Statement, statements, tokens)

    Flow[Token]
      .scan((Option.empty[Iterator[Token]], state)) {
        case ((_, af@AdviceFlowState(token2Statements, _, _)), token) =>
          try {

            val tokens = af.token + token

            af.nToStatement.keys.toList.sorted.headOption match {
              case Some(n) =>
                println(s"Try ${vocabulary.wordMap(token)} n=$n")
                val nToStatement = calcRest(af.nToStatement.values.flatten.toList, tokens)

                //println(s"statement(1) = ${nToStatement(1)}")
                println(s"size = ${nToStatement.size}")
                val iterator = nToStatement.keys.toList.sorted.headOption match {
                  case Some(x) =>
                    nToStatement(x)
                      .flatMap { statement =>
                        statement.filterNot(tokens)
                      }
                      .groupBy(identity)
                      .map {
                        case (token, tokens) =>
                          token -> tokens.length
                      }
                      .toList
                      .sortBy {
                        -_._2
                      }
                      .map(_._1)
                      .map { x =>
                        println(s"Adviced word == $x -> ${vocabulary.wordMap(x)}")
                        x
                      }
                      .toIterator
                  case None =>
                    Iterator.empty

                }
                (Some(iterator), af.copy(nToStatement = nToStatement, token = tokens))

              case None =>
                (None, af.copy(token = tokens))
            }
          }
          catch {
            case th: Throwable =>
              println(af.nToStatement.keys)
              th.printStackTrace()
              throw th
          }
        //          (Some(Iterator.single(token)), af)
      }
      .collect {
        case (Some(x), _) =>
          x
      }
  }

  private def calcRest(statements: List[Statement], tokens: Set[Token]) =
    statements
      .groupBy { statement =>
        statement.filterNot(tokens).distinct.length
      }
      .filter(_._1 > 0)

  case class AdviceFlowState(token2Statements: Map[Token, List[Statement]],
                             nToStatement: Map[Int, List[Statement]],
                             token: Set[Token])


}
