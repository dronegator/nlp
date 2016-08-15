package com.github.dronegator.nlp.main

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Flow, Source}
import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.splitter.Splitter
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenMap}
import com.github.dronegator.nlp.utils.CFG

import scala.concurrent.{Await, duration}, duration._

/**
 * Created by cray on 8/15/16.
 */
object NLTPMainStream extends App {

  val Array(file) = args

  val cfg = CFG()

  val source = io.Source.fromFile(new File(file)).getLines()

  implicit val context = scala.concurrent.ExecutionContext.global

  implicit val system = ActorSystem()

  implicit val mat = ActorMaterializer()

  val splitter = new Splitter(cfg)

  val tokenizer = new Tokenizer(cfg, None)

  val phraseDetector = new PhraseDetector(cfg)

  val accumulator = new Accumulator(cfg, phraseDetector)

  val map = Tokenizer.MapOfPredefs

  val n = map.valuesIterator.flatten.max

  val tokenVariances =
    Flow[(Map[String, List[Int]], Int, List[Tokenizer.Token])].
      collect {
        case (_, _, z) => z
      }.
      zipWith(Source.fromIterator(() => Iterator.from(1))) {
        case (tokens, n) =>
          println(f"$n%-10d : ${tokens.mkString(" :: ")}")
          tokens
      }.
      scan((List.empty[List[Token]], Option.empty[List[Token]]))(accumulator(_,_)).
      collect {
        case (_, Some(phrase)) => phrase
      }.toMat(Sink.fold(List.empty[List[Token]]) {
      case (list, x) => x :: list
    })(Keep.right)

  val maps =
    Flow[(Map[String, List[Int]], Int, List[Tokenizer.Token])].
      collect {
        case (x, y, _) => (x, y)
      }.
      toMat(Sink.
        fold(Option.empty[(TokenMap, Token)]) {
          case (_, x) => Option(x)
        })(Keep.right)

  val (outcome, phrases) =
    (Source.fromIterator(() => source).
      map(splitter(_)).
      mapConcat(_.toList).
      scan((map, n, List[Tokenizer.Token]()))(tokenizer(_, _)).
      alsoToMat(maps)(Keep.right).
      toMat(tokenVariances)(Keep.both).run())

  Await.result(phrases, 30 seconds) foreach { phrase =>
    println(phrase.mkString("", " :: ", " :: Nil"))
  }

  Await.result(outcome, 30 seconds) foreach {
    case (map, token) =>
      map.
        toList.
        sortBy(_._1).
        foreach {
          case (key, value :: _) =>
            println(f"$key%-60s:$value%010d")
        }

      println(s"Last token = $token")
  }

  mat.shutdown()
  system.terminate()
}
