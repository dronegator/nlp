package com.github.dronegator.nlp.vocabulary.ToolMiniLanguage

import akka.stream.scaladsl._
import akka.stream.{FlowShape, OverflowStrategy}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by cray on 9/25/16.
  */
object TraversalComponent extends LazyLogging {
  def apply[U](advice: Flow[Token, Iterator[(Token, Int)], U]) =
    Flow.fromGraph(GraphDSL.create(advice, PriorityQueue())(Keep.both) { implicit b =>
      import GraphDSL.Implicits._
      (advice, priority) =>

        val feedbackBranch = b.add {
          Broadcast[Token](2)
        }

        val feedbackMerge = b.add {
          Merge[QueueMessage](2)
        }

        val feedbackLoop = b.add {
          Flow[Iterator[(Token, Int)]]
            .buffer(1, OverflowStrategy.backpressure)
            .map { x =>
              logger.debug(s"Buffer in=$x")
              x
            }
            .expand { x =>
              x.toIterator
            }
            .map { x =>
              logger.debug(s"Buffer out=$x")
              x
            }
            .map { x =>
              QueueMessageAdd(x._1, x._2)
            }
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
                logger.debug(s"into buffer = $token")
                token
            }
        }

        val init = b.add {
          Flow[Token]
            .map[QueueMessage](QueueMessageAdd(_, 10000000))
            .concat(Source.single[QueueMessage](QueueMessageGet))
        }

        init ~> feedbackMerge ~> priority ~> unique ~> feedbackBranch

        feedbackMerge <~ feedbackLoop <~ advice <~ feedbackBranch

        FlowShape(init.in, feedbackBranch.out(1))
    }).watchTermination() {
      case ((left, killSwitch), right) =>

        right.onComplete { _ =>
          killSwitch.shutdown()
        }

        right.onFailure {
          case th =>
            killSwitch.abort(th)
        }

        left
    }


}
