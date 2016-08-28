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

import scala.concurrent.{Future, Await}
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

    val status = (futureNGram1 zip futureNGram2 zip futureNGram3 zip futurePhraseCorrelationRepeated zip futurePhraseCorrelationConsequent zip futureTokenMap zip futurePhrases).
      flatMap {
        case ((((((Some(nGram1), Some(nGram2)), Some(nGram3)), Some((_, phraseCorrelationRepeated))), Some((_, phraseCorrelationConsequent))), Some((tokenMap, lastToken))), phrases) =>

          val vocabularyRaw = VocabularyRawImpl(phrases, nGram1, nGram2, nGram3, tokenMap, phraseCorrelationConsequent, phraseCorrelationInnerTool.init)

          val futureDump = Future {
            lazy val vocabulary: VocabularyImpl = vocabularyRaw

//            println("== N gramm, n = 1 ==")
//            dump(vocabularyRaw.nGram1)
//
//            println("== N gramm, n = 2 ==")
//            dump(vocabularyRaw.nGram2)
//
//            println("== N gramm, n = 3 ==")
//            dump(vocabularyRaw.nGram3)
//
//            println("== Phrases ==")
//            dump(vocabularyRaw.phrases)
//
//            println("== Tokens ==")
//            dump(vocabularyRaw.tokenMap, lastToken)

            println("== Words, repeated in consequent phrases ==")
            dump(phraseCorrelationRepeated, vocabulary.wordMap)

//            println("== Correlation of words in consequent phrases ==")
//            dump(phraseCorrelationConsequent)
          }

          println("Saving the vocabulary")
          save(new File(fileOut), vocabularyRaw.copy(
            phraseCorrelationConsequent = phraseCorrelationConsequentTool.init._2
          ))
          println("The vocabulary has been saved")

          futureDump map {
            _ => 0
          }
        case _ =>
          println("Process has finished with an uncompleted outcome")
          Future.successful(1)
      }

    Await.result(status, Duration.Inf)
  } finally {
    mat.shutdown()
    system.terminate()
  }
}
