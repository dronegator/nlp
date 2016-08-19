package com.github.dronegator.nlp.main

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import com.github.dronegator.nlp.component.TwoPhraseCorelator
import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.ngramscounter.{NGramsCounter}
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.splitter.Splitter
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Word, Token, TokenMap}
import com.github.dronegator.nlp.component.twophrases.TwoPhrases
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.vocabulary.{VocabularyImpl, Vocabulary, VocabularyRawImpl}

import scala.concurrent.duration._
import scala.concurrent.{Await, duration}

/**
 * Created by cray on 8/15/16.
 */
object NLPTMainStream
  extends App
  with MainTools {

  val Array(fileIn, fileOut) = args

  val cfg = CFG()

  val source = io.Source.fromFile(new File(fileIn)).getLines()

  implicit val context = scala.concurrent.ExecutionContext.global

  implicit val system = ActorSystem()

  implicit val mat = ActorMaterializer()

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

  val twoPhrasesVoc = Flow[List[Token]].
    fold(TwoPhrases.Init)(twoPhrases(_, _)).
    toMat(Sink.headOption)(Keep.right)

  val twoPhraseCorelatorVoc = Flow[List[Token]].
    fold(TwoPhraseCorelator.Init)(twoPhraseCorelator(_, _)).
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
      alsoToMat(twoPhrasesVoc)(Keep.both).
      alsoToMat(twoPhraseCorelatorVoc)(Keep.both).
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


  def plain[A,B,C](x: (A, B), y:C) = (x._1, x._2, y)
  def plain[A,B,C,D](x: (A, B, C), y:D ) = (x._1, x._2, x._3, y)
  def plain[A,B,C,D, E](x: (A, B, C, D), y: E ) = (x._1, x._2, x._3, x._4, y)

  try {
    val (futureToToken, (((((ng1, ng2), ng3), twoPhrasesOut),twoPhraseCorelatorOut), futurePhrases)) =
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

    val Some(ngram1) = Await.result(ng1, Duration.Inf)

    val Some(ngram2) = Await.result(ng2, Duration.Inf)

    val Some(ngram3) = Await.result(ng3, Duration.Inf)

    val Some(twoPhrasesOut1) = Await.result(twoPhrasesOut, Duration.Inf)

    val Some(twoPhraseCorelatorOut1) = Await.result(twoPhraseCorelatorOut, Duration.Inf)

    val Some((toToken, lastToken)) = Await.result(futureToToken, Duration.Inf)

    val phrases = Await.result(futurePhrases, Duration.Inf)

    val vocabularyRaw = VocabularyRawImpl(phrases, ngram1, ngram2, ngram3, toToken, twoPhraseCorelatorOut1._2)

    val vocabulary: VocabularyImpl = vocabularyRaw

    println("== 1 gramm ==")
   // dump(ngram1)

    println("== 2 gramm ==")
   // dump(ngram2)

    println("== 2 gramm ==")
   // dump(ngram3)

    println("== phrases ==")
   // dump(phrases)

   // dump(toToken, lastToken)

    println("== two phrases ==")

    dump(twoPhrasesOut1._2, vocabulary.toWord)

    println("Corr")

    dump(twoPhraseCorelatorOut1._2)

    save(new File(fileOut), vocabularyRaw)

  } finally {
    mat.shutdown()
    system.terminate()
  }
}
