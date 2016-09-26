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
    val statements = calcRest1(vocabulary.statements, tokens)

    val token2Statement =
      vocabulary.statements
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

            println(s"Try ${vocabulary.wordMap(token)}")
            val tokens = af.token + token

            val n = af.nToStatement.keys.toList.sorted.head
            val nToStatement = calcRest(af.nToStatement(n), tokens)

            //println(s"statement(1) = ${nToStatement(1)}")
            val iterator = nToStatement(nToStatement.keys.toList.sorted.head)
              .flatMap { statement =>
                println(s"Statement!")
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
              .toIterator
              .map(_._1)
              .map { x =>
                println(s"Adviced word == ${vocabulary.wordMap(x)}")
                x
              }

            (Some(iterator), af.copy(nToStatement = nToStatement, token = tokens))
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
        println(s"statement === $statement")
        statement.filterNot(tokens).distinct.length -> statement
      }
      .map {
        case ((n, _), statements) =>
          n -> statements
      }

  private def calcRest1(statements: List[Statement], tokens: Set[Token]) =
    statements
      .groupBy { statement =>
        statement.filterNot(tokens).distinct.length -> statement
      }
      .map {
        case ((n, _), statements) =>
          n -> statements
      }

  case class AdviceFlowState(token2Statements: Map[Token, List[Statement]],
                             nToStatement: Map[Int, List[Statement]],
                             token: Set[Token])

}
