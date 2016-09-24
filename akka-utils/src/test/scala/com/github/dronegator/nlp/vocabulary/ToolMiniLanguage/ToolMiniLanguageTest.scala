package com.github.dronegator.nlp.vocabulary.ToolMiniLanguage

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.utils.stream._
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

/**
 * Created by cray on 9/23/16.
 */
class ToolMiniLanguageTest extends WordSpec with Matchers {

  trait mocks {
    lazy implicit val context = ExecutionContext.Implicits.global

    lazy implicit val system = ActorSystem()

    lazy implicit val mat = ActorMaterializer()

    lazy val tokens = Source.fromIterator(() => Iterator.range(1, 200))
      .trace("input tokens:")

    lazy val advice = Flow[Token]
      .trace("an advice for")
      .map { x =>
        (x to x + 200).filter(_ < 150).toIterator.filter(_ % 2 == 0)
      }

    lazy val concat = Sink.fold[List[Token], Token](List.empty[Token]){
      case (tokens, token) =>
        println(s"token = $token")
        token :: tokens
    }

    lazy val flow = Flow.fromGraph(TraversalComponent(advice))
  }

  "traverseComponent" should {
    "complete with success" in new mocks {
      val list = Await.result(tokens.via(flow).runWith(concat), 20 seconds)
      println(list)
    }
  }

  "priorityQueue" should {
    val priorityQueue = Flow.fromGraph(PriorityQueue())

    "complete with success" ignore new mocks {
      val futureOutcome = tokens.map(QueueMessageAdd(_): QueueMessage)
        .concat(Source.single(QueueMessageGet: QueueMessage))
        .via(priorityQueue)
        .runWith(concat)

      val outcome = Await.result(futureOutcome, 20 seconds)
      println(outcome)
    }
  }
}
