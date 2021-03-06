package com.github.dronegator.nlp.main

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Statement, Token, TokenMap}
import com.github.dronegator.nlp.utils.Match._
import com.github.dronegator.nlp.utils.concurrent.Zukunft
import com.github.dronegator.nlp.utils.stream._
import com.github.dronegator.nlp.vocabulary.{VocabularyHintImpl, VocabularyImpl, VocabularyRawImpl}
import configs.syntax._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
/**
 * Created by cray on 8/15/16.
 */

case class NLPTMainIndexStreamConfig(maximumFrameLength: Int = 1024 * 1025)


object NLPTMainStream
  extends App
  with NLPTAppPartial
    with MainConfig[NLPTMainIndexStreamConfig]
  with Combinators
  with MainTools {

  val fileIn :: fileOut ::  OptFile(hints) = args.toList

  lazy val cfg = config.get[NLPTMainIndexStreamConfig]("index-stream").value

  lazy val vocabularyHint = hints.map(load(_): VocabularyImpl).getOrElse{
    println("Hints have initialized")
    VocabularyHintImpl(Tokenizer.MapOfPredefs, Map())
  }

  val source =
    FileIO.fromPath(Paths.get(fileIn)).
      progress().
      via(Framing.delimiter(ByteString("\n"), cfg.maximumFrameLength, allowTruncation = true)).
      map(_.utf8String).
      //trace("Input string: ").
      //monitor()(Keep.right).
      watchTermination()(Keep.right)

  implicit val context = scala.concurrent.ExecutionContext.global

  implicit val system = ActorSystem()

  implicit val mat = ActorMaterializer()

  val nGram1Flow = Flow[Statement].
    component(nGram1Tool).
    toMat(Sink.headOption)(Keep.right)

  val nGram2Flow = Flow[Statement].
    component(nGram2Tool).
    toMat(Sink.headOption)(Keep.right)

  val nGram3Flow = Flow[Statement].
    component(nGram3Tool).
    toMat(Sink.headOption)(Keep.right)

  val phraseCorrelationRepeatedFlow = Flow[Statement].
    take(0).
    component(phraseCorrelationRepeatedTool).
    toMat(Sink.headOption)(Keep.right)

  val phraseCorrelationConsequentFlow = Flow[Statement].
    //take(0).
    component(phraseCorrelationConsequentTool).
    toMat(Sink.headOption)(Keep.right)

   val phraseCorrelationInnerFlow = Flow[Statement].
    //take(0).
    component(phraseCorrelationInnerTool).
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
      component(accumulatorTool).
      filter { x =>
        x.length > 4 && x.length < 25
      }.
      alsoToMat(nGram1Flow)(Keep.both).
      alsoToMat(nGram2Flow)(Keep.both).
      alsoToMat(nGram3Flow)(Keep.both).
      alsoToMat(phraseCorrelationRepeatedFlow)(Keep.both).
      alsoToMat(phraseCorrelationConsequentFlow)(Keep.both).
      alsoToMat(phraseCorrelationInnerFlow)(Keep.both).
      toMat(Sink.fold(List.empty[Statement]) {
        case (list, x) =>
          x :: list // Collect list of phrases
      })(Keep.both)

  val substitute = Map("’" -> "'") // TODO: Move it into some combinator
  try {
    val ((termination, futureTokenMap),
    (((((((_, futureNGram1), futureNGram2), futureNGram3), futurePhraseCorrelationRepeated), futurePhraseCorrelationConsequent), futurePhraseCorrelationInner), futurePhrases)) =
      (source.
        //        trace("An original string: ").
        //                map { x =>
        //                  x.toLowerCase
        //                }.
        component(splitterTool).mapConcat(_.toList).
        map(x => substitute.getOrElse(x, x)).
        //trace("A word after substitution: ").
        componentScan(tokenizerTool).
        alsoToMat(tokenMapSink)(Keep.both).
        toMat(tokenSink)(Keep.both).run())

    println(s"Stream has finished with ${termination.recover{
      case th: Throwable =>
        th.printStackTrace()
        th
    }.await}")

    val status = (futureNGram1 zip futureNGram2 zip futureNGram3 zip futurePhraseCorrelationRepeated zip futurePhraseCorrelationConsequent zip futurePhraseCorrelationInner zip futureTokenMap zip futurePhrases).
      flatMap {
        case (((((((Some(nGram1), Some(nGram2)), Some(nGram3)), Some(phraseCorrelationRepeated)), Some(phraseCorrelationConsequent)), Some(phraseCorrelationInner)), Some((tokenMap, lastToken))), phrases) =>

          val vocabularyRaw = VocabularyRawImpl(tokenMap, vocabularyHint.meaningMap,  phrases, nGram1, nGram2, nGram3,  phraseCorrelationRepeated, phraseCorrelationConsequent, phraseCorrelationInner)


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

            println("== Correlation of words in consequent phrases ==")
            dump(phraseCorrelationConsequent)
          }

          println("Saving the vocabulary")
          save(new File(fileOut), vocabularyRaw/*.copy(
            phraseCorrelationConsequent = phraseCorrelationConsequentTool.init._2
          )*/)
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
