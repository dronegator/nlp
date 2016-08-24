package com.github.dronegator.nlp.main

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.dronegator.nlp.component.TwoPhraseCorelator
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenMap}
import com.github.dronegator.nlp.component.twophrases.TwoPhrases
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.vocabulary.{VocabularyImpl, VocabularyRawImpl}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by cray on 8/15/16.
 */
object NLPTMainStream
  extends App
  with MainTools {

  val Array(fileIn, fileOut) = args
  val cfg = CFG()

  def progress[A](chunk: Int = 1024*10) = Flow[A].
    scan((0, Option.empty[A])) {
      case ((n, _), item) =>
        if (n % chunk == 0)                         {
          println(f"$n%20d items passed through ${Runtime.getRuntime().freeMemory()} ${Runtime.getRuntime().maxMemory()}")
        }

        val m = item           match {
          case x: Seq[_] => n + x.length
          case _ => n + 1
        }

        (m, Some(item))
    }.
    collect {
      case (_, Some(item)) =>
        item
    }

  val source =
    FileIO.fromPath(Paths.get(fileIn)).
      via(progress()).
      via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024*1025, allowTruncation = false)).
      map(_.utf8String).
      //monitor()(Keep.right).
      watchTermination()(Keep.right)

  //io.Source.fromFile(new File(fileIn)).getLines()

  implicit val context = scala.concurrent.ExecutionContext.global

  implicit val system = ActorSystem()

  implicit val mat = ActorMaterializer()

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
          //println(f"$n%-10d : ${tokens.mkString(" :: ")}")
          tokens
      }.
      scan((List.empty[List[Token]], Option.empty[List[Token]]))(accumulator(_, _)).
      collect {
        case (_, Some(phrase)) => phrase
      }.
      alsoToMat(count1gramms)(Keep.both).
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


  def plain[A, B, C](x: (A, B), y: C) = (x._1, x._2, y)

  def plain[A, B, C, D](x: (A, B, C), y: D) = (x._1, x._2, x._3, y)

  def plain[A, B, C, D, E](x: (A, B, C, D), y: E) = (x._1, x._2, x._3, x._4, y)

  val substitute = Map("’"->"'")
  try {
    val ((termination, futureToToken), ((((((_, ng1), ng2), ng3), twoPhrasesOut), twoPhraseCorelatorOut), futurePhrases)) =
      (source.
        /*map { x =>
          println(x)
          x
        }.*/
        map(splitter(_)).
        mapConcat(_.toList).
        map(x=>substitute.getOrElse(x,x)).
//        map{ x =>
//          println(x)
//          x
//        }.
        scan(Tokenizer.Init)(tokenizer(_, _)).
        alsoToMat(maps)(Keep.both).
        toMat(tokenVariances)(Keep.both).run())


    println(s"Stream has finished with ${Await.result(termination, Duration.Inf)}")

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

    //dump(twoPhraseCorelatorOut1._2)

    save(new File(fileOut), vocabularyRaw.copy(
      twoPhraseCorelator = TwoPhraseCorelator.Init._2
    ))

  } finally {
    mat.shutdown()
    system.terminate()
  }
}
