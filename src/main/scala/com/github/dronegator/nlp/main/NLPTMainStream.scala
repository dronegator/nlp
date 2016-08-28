package com.github.dronegator.nlp.main

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Phrase, Token, TokenMap}
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

  val nGram1Flow = Flow[Phrase].
    componentFold(nGram1Tool).
    toMat(Sink.headOption)(Keep.right)

  val nGram2Flow = Flow[Phrase].
    componentFold(nGram2Tool).
    toMat(Sink.headOption)(Keep.right)

  val nGram3Flow = Flow[Phrase].
    componentFold(nGram3Tool).
    toMat(Sink.headOption)(Keep.right)

  val phraseCorrelationRepeatedFlow = Flow[Phrase].
    componentFold(phraseCorrelationRepeatedTool).
    toMat(Sink.headOption)(Keep.right)

  val phraseCorrelationConsequentFlow = Flow[Phrase].
    componentFold(phraseCorrelationConsequentTool).
    toMat(Sink.headOption)(Keep.right)

  val tokenMapSink =
    Flow[Tokenizer.Init].
      collect {
        case (x, y, _) => (x, y)
      }.
      toMat(Sink.
        fold(Option.empty[(TokenMap, Token)]) {
          case (_, x) => Option(x)
        })(Keep.right)

  val tokenSink =
    Flow[Tokenizer.Init].
      collect {
        case (_, _, z) => z
      }.
      componentScan(accumulatorTool).
      collect {
        case (_, Some(phrase)) => phrase
      }.
      alsoToMat(nGram1Flow)(Keep.both).
      alsoToMat(nGram2Flow)(Keep.both).
      alsoToMat(nGram3Flow)(Keep.both).
      alsoToMat(phraseCorrelationRepeatedFlow)(Keep.both).
      alsoToMat(phraseCorrelationConsequentFlow)(Keep.both).
      toMat(Sink.fold(List.empty[Phrase]) {
        case (list, x) =>
          x :: list // Collect list of phrases
      })(Keep.both)

  val substitute = Map("’" -> "'") // TODO: Move it into some combinator
  try {
    val ((termination, futureTokenMap),
    ((((((_, futureNGram1), futureNGram2), futureNGram3), futurePhraseCorrelationRepeated), futurePhraseCorrelationConsequent), futurePhrases)) =
      (source.
        //trace("An original string: ").
        component(splitterTool).mapConcat(_.toList).
        map(x => substitute.getOrElse(x, x)).
        //trace("A word after substitution: ").
        componentScan(tokenizerTool).
        alsoToMat(tokenMapSink)(Keep.both).
        toMat(tokenSink)(Keep.both).run())

    println(s"Stream has finished with ${Await.result(termination, Duration.Inf)}")

    val Some(nGram1) = Await.result(futureNGram1, Duration.Inf)

    val Some(nGram2) = Await.result(futureNGram2, Duration.Inf)

    val Some(nGram3) = Await.result(futureNGram3, Duration.Inf)

    val Some(phraseCorrelationRepeated) = Await.result(futurePhraseCorrelationRepeated, Duration.Inf)

    val Some(phraseCorrelationConsequent) = Await.result(futurePhraseCorrelationConsequent, Duration.Inf)

    val Some((tokenMap, lastToken)) = Await.result(futureTokenMap, Duration.Inf)

    val phrases = Await.result(futurePhrases, Duration.Inf)

    val vocabularyRaw = VocabularyRawImpl(phrases, nGram1, nGram2, nGram3, tokenMap, phraseCorrelationConsequent._2, phraseCorrelationInnerTool.init)

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

    dump(phraseCorrelationRepeated._2, vocabulary.wordMap)

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
