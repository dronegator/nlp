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
          .scan((Option.empty[Token], SortedSet.empty[(Int, Int, Token)], Map.empty[Token, (Int, Int)], 0, false)) {
            case ((_, queue, map, nextNumber, _), QueueMessageAdd(token, p)) =>
              logger.debug(s"Add $token")
              map.get(token) match {
                case Some((priority, number)) =>
                  val newPriority = priority - p
                  (None, queue - ((priority, number, token)) + ((newPriority, number, token)), map + (token -> (newPriority, number)), nextNumber, false)

                //                case None if queue.size < 10 =>
                //                  (None, queue + ((-10, nextNumber, token)), map + (token -> (-10, nextNumber)), nextNumber - 1, false)

                case None =>
                  (None, queue + ((-p, nextNumber, token)), map + (token -> (-p, nextNumber)), nextNumber - 1, false)
              }

            case ((_, queue, map, nextNumber, _), QueueMessageGet) if queue.nonEmpty =>
              logger.debug("Get token")
              val firstKey@(_, _, token) = queue.firstKey
              logger.debug(s"Give token=$token, size=${queue.size}")

              (Some(token), queue - firstKey, map - token, nextNumber, false)

            case ((_, queue, map, nextNumber, _), QueueMessageGet) =>
              logger.debug("Get token from empty queue")
              (None, queue, map, nextNumber, true)
          }
          //        .map { x =>
          //          logger.debug(s"$x")
          //          x
          //        }
          .takeWhile(!_._5)
          .collect {
            case (Some(token), _, _, _, _) =>
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
