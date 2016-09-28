package com.github.dronegator.nlp.vocabulary.ToolMiniLanguage

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.Vocabulary
import com.typesafe.scalalogging.LazyLogging

import scala.collection.immutable.TreeMap

/**
 * Created by cray on 9/26/16.
 */
object AdviceFlow extends LazyLogging {
  type StatementId = Int
  type StatementTokens = Set[Token]

  case class AdviceFlowState(id2Statment: Map[StatementId, StatementTokens],
                             token2Statement: Map[Token, Set[StatementId]],
                             size2Statement: TreeMap[Int, Set[StatementId]],
                             token: Set[Token],
                             nextStatementId: StatementId)

  def state(vocabulary: Vocabulary) =
    vocabulary.statements
      .filter { x =>
        x.length > 5 && x.length < 15
      }
      .foldLeft(AdviceFlowState(Map(), Map(), TreeMap(), Set(), 1)) {
        case (af, statement) =>
          val statementId = af.nextStatementId
          val statementTokens = statement.toSet
          AdviceFlowState(
            id2Statment = af.id2Statment + (statementId -> statementTokens),
            statementTokens
              .foldLeft(af.token2Statement) {
                case (map, token) =>
                  map + {
                    token -> (map.getOrElse(token, Set()) + statementId)
                  }
              },
            af.size2Statement + (statementTokens.size -> (af.size2Statement.getOrElse(statementTokens.size, Set()) + statementId)),
            af.token,
            nextStatementId = af.nextStatementId + 1
          )
      }

  def apply(vocabulary: Vocabulary, tokens: Set[Token]): Flow[Token, Iterator[Token], NotUsed] = {

    Flow[Token]
      .scan((Option.empty[Iterator[(Token, Int)]], state(vocabulary))) {
        case ((_, af), token) =>
          try {
            val changedStatement = af.token2Statement.getOrElse(token, Set())

            val id2Statement = af.id2Statment ++
              changedStatement
                .toIterator
                .map { statementId =>
                  statementId -> (af.id2Statment.getOrElse(statementId, Set()) - token)
                }
                .toMap

            val size2Statement = changedStatement.toIterator
              .foldLeft(af.size2Statement) {
                case (size2Statement, statementId) =>
                  val size = af.id2Statment.getOrElse(statementId, Set()).size

                  size2Statement +
                    (size -> (size2Statement.getOrElse(size, Set()) - statementId)) +
                    ((size - 1) -> (size2Statement.getOrElse(size, Set()) + statementId))
              } - 0

            val nextTokens = size2Statement.headOption.map {
              case (size, statements) =>
                println(s"headOption=$size statements=${statements.size} token=$token word=${vocabulary.wordMap.getOrElse(token, "***")}")
                statements
                  .toIterator
                  .flatMap { statementId =>
                    id2Statement.getOrElse(statementId, Set())
                  }
                  .foldLeft(Map[Token, Int]()) {
                    case (map, token) =>
                      map + (token -> (map.getOrElse(token, 0) + 1))
                  }
                  .toList
                  .sortBy(-_._2)
                  .toIterator
            }

            (nextTokens, af.copy(
              id2Statment = id2Statement,
              token2Statement = af.token2Statement - token,
              size2Statement = size2Statement,
              token = af.token + token
            ))
          }
          catch {
            case th: Throwable =>
              th.printStackTrace()
              throw th
          }
      }
      .collect {
        case (Some(x), _) =>
          x.map(_._1)
      }
  }

  private def calcRest(statements: List[Statement], tokens: Set[Token]) =
    statements
      .groupBy { statement =>
        statement.filterNot(tokens).distinct.length
      }
      .filter(_._1 > 0)


}
