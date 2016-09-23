package com.github.dronegator.nlp.vocabulary

/**
 * Created by cray on 9/22/16.
 */


import akka.stream.scaladsl._
import akka.stream.{FlowShape, OverflowStrategy}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.VocabularyTools.VocabularyTools

import scala.collection.SortedSet
import scala.concurrent.duration._

object ToolMiniLanguageTrait {

  def traversalComponent[U](advice: Flow[Token, Token, U]) =
    GraphDSL.create(advice) { implicit b =>
      import GraphDSL.Implicits._
      (advice) =>

        val input = b.add {
          Flow[Token]
            .map { x => QueueMessageAdd(x): QueueMessage }
        }

        val priorityQueue = b.add {
          Flow[QueueMessage]
            .scan((Option.empty[Token], SortedSet.empty[(Int, Token)], Map.empty[Token, Int], false)) {
              case ((_, queue, map, _), QueueMessageAdd(token)) =>
                println(s"Add $token")
                map.get(token) match {
                  case Some(priority) =>
                    val newPriority = priority - 1
                    (None, queue - ((priority, token)) + ((newPriority, token)), map + (token -> newPriority), false)

                  case None =>
                    (None, queue + ((0, token)), map + (token -> 0), false)
                }

              case ((_, queue, map, _), QueueMessageGet) if queue.nonEmpty =>
                println("Get token")
                val firstKey@(_, token) = queue.firstKey
                println(s"Give token=$token")

                (Some(token), queue - firstKey, map - token, false)

              case ((_, queue, map, _), QueueMessageGet) =>
                println("Get token from empty queue")
                (None, queue, map, true)
            }
            .map { x =>
              println(x)
              x
            }
            .takeWhile(!_._4)
            .collect {
              case (Some(token), _, _, _) =>
                token
            }
        }

        val branchOut = b.add {
          Broadcast[Token](2)
        }

        val feedbackGet = b.add {
          Broadcast[Token](2)
        }

        val mergeIn = b.add {
          Merge[Token](2)
        }

        val mergeGet = b.add {
          MergePreferred[QueueMessage](2)
        }

        val unique = b.add {
          Flow[Token]
            .scan((Option.empty[Token], Set.empty[Token])) {
              case ((_, set), token) if set.contains(token) =>
                (Option.empty[Token], set)

              case ((_, set), token) =>
                (Some(token), set + token)

            }
            .collect {
              case (Some(token), _) =>
                println(s"into buffer = $token")
                token
            }
        }

        val buffer = b.add {
          Flow[Token].buffer(3, OverflowStrategy.backpressure)
        }

        mergeIn ~> input ~> mergeGet.preferred

        val log1 = Flow[Token].
          map { x =>
            println(s"in bufer $x")
            x
          }

        val log2 = Flow[Token].
          map { x =>
            println(s"out bufer $x")
            x
          }

        mergeGet ~> priorityQueue ~> feedbackGet ~> unique ~> branchOut ~> advice ~> log1 ~> buffer ~> log2 ~> mergeIn

        feedbackGet.out(1)
          .buffer(1, OverflowStrategy.backpressure)
          .map { x =>
            println(s"feedback = $x")
            QueueMessageGet
          } ~> mergeGet

        Source.tick(1 second, 100 millisecond, QueueMessageGet)
          .map { x =>
            println(s"get $x")
            x
          }
          .take(1000) ~> mergeGet

        FlowShape(mergeIn.in(1), branchOut.out(1))
    }

  sealed trait QueueMessage

  case class QueueMessageAdd(token: Token) extends QueueMessage

  case object QueueMessageGet extends QueueMessage

}

trait ToolMiniLanguageTrait {
  this: VocabularyTools =>

  def vocabulary: Vocabulary

  def miniLanguage(keywords: Set[Token]): Vocabulary = ???

  def miniLanguageKeywords(keywords: Set[Token]) =
    Source.fromIterator(() => keywords.toIterator)

}
