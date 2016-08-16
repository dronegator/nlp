package com.github.dronegator.nlp.main

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.ngramscounter.{NGramsCounter}
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.splitter.Splitter
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenMap}
import com.github.dronegator.nlp.utils.CFG

import scala.concurrent.duration._
import scala.concurrent.{Await, duration}

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

  val ngramms1 = new NGramsCounter(cfg, 1)

  val ngramms2 = new NGramsCounter(cfg, 2)

  val ngramms3 = new NGramsCounter(cfg, 3)

  val map = Tokenizer.MapOfPredefs

  val n = map.valuesIterator.flatten.max

  val count1gramms = Flow[List[Token]].
    fold(Map[List[Token], Int]())(ngramms1(_, _)).
    toMat(Sink.headOption)(Keep.right)

  val count2gramms = Flow[List[Token]].
    fold(Map[List[Token], Int]())(ngramms2(_, _)).
    toMat(Sink.headOption)(Keep.right)

  val count3gramms = Flow[List[Token]].
    fold(Map[List[Token], Int]())(ngramms3(_, _)).
    toMat(Sink.headOption)(Keep.right)

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
      scan((List.empty[List[Token]], Option.empty[List[Token]]))(accumulator(_, _)).
      collect {
        case (_, Some(phrase)) => phrase
      }.
      alsoToMat(count1gramms)(Keep.right).
      alsoToMat(count2gramms)(Keep.both).
      alsoToMat(count3gramms)(Keep.both).
      toMat(Sink.fold(List.empty[List[Token]]) {
        case (list, x) => x :: list
      })(Keep.both)

  val maps =
    Flow[(Map[String, List[Int]], Int, List[Tokenizer.Token])].
      collect {
        case (x, y, _) => (x, y)
      }.
      toMat(Sink.
        fold(Option.empty[(TokenMap, Token)]) {
          case (_, x) => Option(x)
        })(Keep.right)


  try {
    val (outcome, (((ng1, ng2), ng3), phrases)) =
      (Source.fromIterator(() => source).
        /*map { x =>
          println(x)
          x
        }.*/
        map(splitter(_)).
        mapConcat(_.toList).
        scan((map, n, List[Tokenizer.Token]()))(tokenizer(_, _)).
        alsoToMat(maps)(Keep.right).
        toMat(tokenVariances)(Keep.both).run())

    implicit val ordering = Ordering.
      fromLessThan((x: List[Int], y: List[Int]) => (x zip y).find(x => x._1 != x._2).map(x => x._1 < x._2).getOrElse(false))

    Await.result(ng1, Duration.Inf) foreach { map =>
      println("== 1 gramm ==")
      map.toList.sortBy(_._1).foreach {
        case (key, value) =>
          println(s" ${key.map(_.toString).mkString("", " :: ", " :: Nil")} -> $value")
      }
    }

    Await.result(ng2, Duration.Inf) foreach { map =>
      println("== 2 gramm ==")
      map.toList.sortBy(_._1).foreach {
        case (key, value) =>
          println(s" ${key.map(_.toString).mkString("", " :: ", " :: Nil")} -> $value")
      }
    }

    Await.result(ng3, Duration.Inf) foreach { map =>
      println("== 3 gramm ==")
      map.toList.sortBy(_._1).foreach {
        case (key, value) =>
          println(s" ${key.map(_.toString).mkString("", " :: ", " :: Nil")} -> $value")
      }
    }

    Await.result(phrases, Duration.Inf) foreach { phrase =>
      println(phrase.mkString("", " :: ", " :: Nil"))
    }

    Await.result(outcome, Duration.Inf) foreach {
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
  } finally {
    mat.shutdown()
    system.terminate()
  }
}
