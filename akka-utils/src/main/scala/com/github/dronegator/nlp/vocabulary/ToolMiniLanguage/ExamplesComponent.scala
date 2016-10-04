package com.github.dronegator.nlp.vocabulary.ToolMiniLanguage

import akka.NotUsed
import akka.stream.scaladsl._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils._
import com.github.dronegator.nlp.vocabulary.Vocabulary
import com.github.dronegator.nlp.vocabulary.VocabularyTools._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.immutable.TreeMap

/**
  * Created by cray on 10/2/16.
  */
object ExampleFlow extends LazyLogging {
  type StatementId = Int
  type StatementTokens = Set[Token]

  case class ExampleFlowState(id2Statment: Map[StatementId, StatementTokens],
                              token2Statement: Map[Token, Set[StatementId]],
                              size2Statement: TreeMap[Int, Set[StatementId]],
                              token: Set[Token],
                              nextStatementId: StatementId,
                              countStatement: Int,
                              statement: Set[StatementId],
                              id2StatementOrig: Map[StatementId, Statement])

  def state(vocabulary: Vocabulary) =
    vocabulary.statements
      .filter { x =>
        x.length > 4 && x.length < 15
      }
      .distinct
      .foldLeft(ExampleFlowState(Map(), Map(), TreeMap(), Set(), 1, 0, Set(), Map())) {
        case (af, statement) =>
          val statementId = af.nextStatementId
          val statementTokens = statement.toSet
          ExampleFlowState(
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
            nextStatementId = af.nextStatementId + 1,
            0,
            Set(),
            id2StatementOrig = af.id2StatementOrig + (statementId -> statement)
          )
      }

  def apply(vocabulary: Vocabulary): Flow[Token, (Token, List[Statement]), NotUsed] = {
    Flow[Token]
      .scan((Option.empty[(Token, List[Statement])], state(vocabulary))) {
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
                    ((size - 1) -> (size2Statement.getOrElse(size - 1, Set()) + statementId))
              }

            val (statement, statementAsExample) = size2Statement.headOption.map {
              case (size, newStatement) =>
                logger.info(s"headOption=$size statements=${newStatement.size} token=$token word=${vocabulary.wordMap.getOrElse(token, "***")}")

                val statement = ((af.statement ++ newStatement) & af.token2Statement.getOrElse(token, Set()))
                  .iterator
                  .flatMap(x => af.id2StatementOrig.get(x).map(x -> _))
                  .collect {
                    case (statementId, statement) if statement.size > 7 && !(statement contains 76) =>
                      //                      println(af.id2Statment(statementId).flatMap(x => vocabulary.wordMap.get(x)))
                      //                      println(vocabulary.untokenize(statement))
                      val probabilityStatement = vocabulary.probability(statement) / vocabulary.statementDenominator(statement)
                      val probability3Gram = statement
                        .sliding(3)
                        .collect {
                          case key@t1 :: `token` :: t3 :: _ =>
                            //vocabulary.map2ToMiddle.get(t1 :: t3 :: Nil)
                            vocabulary.pNGram3.getOrElse(key, {
                              println(s"Can not find word=${vocabulary.wordMap.getOrElse(token, "***")} in ${vocabulary.untokenize(statement)}")
                              (1.0)
                            })
                        }
                        .headOption

                      (statementId, statement, probability3Gram, probabilityStatement)
                  }
                  .collect {
                    case (statementId, statement, Some(probability3Gram), probabilityStatement) =>
                      (statementId, statement, probability3Gram, probabilityStatement)
                  }
                  .sortBy(x => (-x._3 * -x._4))
                  .take(8)
                  .map(x => (x._1, x._2))
                  .toList
                (af.statement ++ newStatement -- statement.map(_._1), statement)
            }.getOrElse((af.statement, List()))

            val nextAf = af.copy(
              id2Statment = id2Statement,
              token2Statement = af.token2Statement - token,
              size2Statement = size2Statement - 0,
              token = af.token + token,
              statement = statement,
              countStatement = af.countStatement + size2Statement.getOrElse(0, Set()).size
            )

            //logger.info(s"tokenSize=${nextAf.token.size}  statementSize=${nextAf.countStatement} token=$token word=${vocabulary.wordMap.getOrElse(token, "***")}")

            (Some((token, statementAsExample.map(_._2).toList)), nextAf)
          }
          catch {
            case th: Throwable =>
              th.printStackTrace()
              throw th
          }
      }
      .collect {
        case (Some(x), _) =>
          //x.map(_._1)
          x
      }
  }

  private def calcRest(statements: List[Statement], tokens: Set[Token]) =
    statements
      .groupBy { statement =>
        statement.filterNot(tokens).distinct.length
      }
      .filter(_._1 > 0)


}
