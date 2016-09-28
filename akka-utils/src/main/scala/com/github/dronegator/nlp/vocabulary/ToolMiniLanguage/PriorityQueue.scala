package com.github.dronegator.nlp.vocabulary.ToolMiniLanguage

import akka.stream.scaladsl._
import akka.stream.{FlowShape, KillSwitches, OverflowStrategy}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.typesafe.scalalogging.LazyLogging

import scala.collection.SortedSet

/**
  * Created by cray on 9/25/16.
  */
object PriorityQueue extends LazyLogging {
  lazy val priorityQueue = GraphDSL.create(KillSwitches.single[QueueMessage]) { implicit b =>
    killSwitch =>
      import GraphDSL.Implicits._

      val priorityQueue = b.add {
        Flow[QueueMessage]
          .scan((Option.empty[Token], SortedSet.empty[(Int, Token)], Map.empty[Token, Int], false)) {
            case ((_, queue, map, _), QueueMessageAdd(token)) =>
              logger.debug(s"Add $token")
              map.get(token) match {
                case Some(priority) =>
                  val newPriority = priority - 1
                  (None, queue - ((priority, token)) + ((newPriority, token)), map + (token -> newPriority), false)

                case None =>
                  (None, queue + ((0, token)), map + (token -> 0), false)
              }

            case ((_, queue, map, _), QueueMessageGet) if queue.nonEmpty =>
              logger.debug("Get token")
              val firstKey@(_, token) = queue.firstKey
              logger.debug(s"Give token=$token, size=${queue.size}")

              (Some(token), queue - firstKey, map - token, false)

            case ((_, queue, map, _), QueueMessageGet) =>
              logger.debug("Get token from empty queue")
              (None, queue, map, true)
          }
          //        .map { x =>
          //          logger.debug(s"$x")
          //          x
          //        }
          .takeWhile(!_._4)
          .collect {
            case (Some(token), _, _, _) =>
              token
          }
      }


      var feedbackMerge = b.add {
        MergePreferred[QueueMessage](1)
      }

      var feedbackBranch = b.add {
        Broadcast[Token](2)
      }

      var feedbackLoop = b.add {
        Flow[Token]
          .buffer(1, OverflowStrategy.backpressure)
          .map { _ =>
            QueueMessageGet
          }
      }


      feedbackMerge ~> killSwitch ~> priorityQueue ~> feedbackBranch

      feedbackMerge <~ feedbackLoop <~ feedbackBranch

      FlowShape(feedbackMerge.preferred, feedbackBranch.out(1))
  }

  def apply() =
    priorityQueue

}
