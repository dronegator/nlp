package com.github.dronegator.nlp.vocabulary

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.utils.stream._
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}

/**
 * Created by cray on 9/23/16.
 */
class ToolMiniLanguageTraitTest extends FlatSpec with Matchers {

  trait mocks {
    lazy implicit val context = ExecutionContext.Implicits.global

    lazy implicit val system = ActorSystem()

    lazy implicit val mat = ActorMaterializer()

    val tokens = Source.fromIterator(() => Iterator.range(1, 200))
      .trace("input tokens:")

    val advice = Flow[Token]
      .trace("an advice for")
      .map { x =>
        (x to x + 200).filter(_ < 150).toIterator //.filter(_ % 2 == 0)
      }

    val concat = Sink.fold[List[Token], Token](List.empty[Token]){
      case (tokens, token) =>
        println(s"token = $token")
        token :: tokens
    }

    val flow = Flow.fromGraph(ToolMiniLanguageTrait.traversalComponent(advice))

  }

  "traverseComponent" should "complete with success" in new mocks {
    val list = Await.result(tokens.via(flow).runWith(concat), 20 seconds)
    println(list)
  }
}
