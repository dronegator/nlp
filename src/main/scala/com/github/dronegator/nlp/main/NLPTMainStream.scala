package com.github.dronegator.nlp.main

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenMap}
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.utils.stream._
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
  lazy val cfg = CFG()

  val source =
    FileIO.fromPath(Paths.get(fileIn)).
      progress().
      via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024*1025, allowTruncation = false)).
      map(_.utf8String).
      //trace("Input string: ").
      //monitor()(Keep.right).
      watchTermination()(Keep.right)

  //io.Source.fromFile(new File(fileIn)).getLines()

  implicit val context = scala.concurrent.ExecutionContext.global

  implicit val system = ActorSystem()

  implicit val mat = ActorMaterializer()

  val count1gramms = Flow[List[Token]].
    //fold(ngramms1.init)(ngramms1).
    componentFold(nGram1Tool).
    toMat(Sink.headOption)(Keep.right)

  val count2gramms = Flow[List[Token]].
    fold(nGram2Tool.init)(nGram2Tool).
    toMat(Sink.headOption)(Keep.right)

  val count3gramms = Flow[List[Token]].
    fold(nGram3Tool.init)(nGram3Tool).
    toMat(Sink.headOption)(Keep.right)

  val twoPhrasesVoc = Flow[List[Token]].
    fold(phraseCorrelationRepeatedTool.init)(phraseCorrelationRepeatedTool).
    toMat(Sink.headOption)(Keep.right)

  val twoPhraseCorelatorVoc = Flow[List[Token]].
    fold(phraseCorrelationConsequentTool.init)(phraseCorrelationConsequentTool).
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
    //  scan(accumulator.init)(accumulator).
      componentScan(accumulatorTool).
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

  val substitute = Map("’" -> "'")
  try {
    val ((termination, futureToToken), ((((((_, ng1), ng2), ng3), twoPhrasesOut), twoPhraseCorelatorOut), futurePhrases)) =
      (source.
        //trace("An original string: ").
        component(splitterTool).
        mapConcat(_.toList).
        map(x => substitute.getOrElse(x, x)).
        //trace("A word after substitution: ").
        componentScan(tokenizerTool).
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

    val vocabularyRaw = VocabularyRawImpl(phrases, ngram1, ngram2, ngram3, toToken, twoPhraseCorelatorOut1._2, phraseCorrelationInnerTool.init)

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
      phraseCorrelationConsequent = phraseCorrelationConsequentTool.init._2
    ))

  } finally {
    mat.shutdown()
    system.terminate()
  }
}
