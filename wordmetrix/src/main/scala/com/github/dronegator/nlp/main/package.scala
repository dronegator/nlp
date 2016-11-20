package com.github.dronegator.nlp

import java.io._

import com.github.dronegator.nlp.component.accumulator.{Accumulator, AccumulatorConfig}
import com.github.dronegator.nlp.component.ngramscounter.{NGramsCounter, NGramsCounterConfig}
import com.github.dronegator.nlp.component.phrase_correlation_consequent.{PhraseCorrelationConsequentWithHints, PhraseCorrelationConsequentWithHintsConfig}
import com.github.dronegator.nlp.component.phrase_correlation_repeated.{PhraseCorrelationInnerWithHints, PhraseCorrelationInnerWithHintsConfig, PhraseCorrelationRepeated, PhraseCorrelationRepeatedConfig}
import com.github.dronegator.nlp.component.phrase_detector.{PhraseDetector, PhraseDetectorConfig}
import com.github.dronegator.nlp.component.splitter.{Splitter, SplitterConfig}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.component.tokenizer.{TokenizerConfig, TokenizerWithHints}
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyHint, VocabularyImplStored, VocabularyRaw}
import com.softwaremill.macwire._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import configs.syntax._
/**
  * Created by cray on 8/17/16.
  */
package object main {

  trait TagHints

  trait NLPTAppPartial
    extends LazyLogging {

    def vocabularyHint: VocabularyHint
  }

  trait NLPTApp
    extends NLPTAppPartial {
    def vocabulary: Vocabulary
  }

  trait Combinators {
    this: NLPTAppPartial =>

    private val config = ConfigFactory.load().getConfig("com.github.dronegator.wordmetrix.component")

    lazy val splitterConfig = config.get[SplitterConfig]("splitter").value

    lazy val splitterTool = wire[Splitter]

    lazy val tokenizerConfig = config.get[TokenizerConfig]("tokenizer-with-hints").value

    lazy val tokenizerTool = wire[TokenizerWithHints]

    lazy val phraseDetectorConfig = config.get[PhraseDetectorConfig]("phrase-detector").value

    lazy val phraseDetectorTool = wire[PhraseDetector]

    lazy val accumulatorConfig = config.get[AccumulatorConfig]("accumulator").value

    lazy val accumulatorTool = wire[Accumulator]

    lazy val nGramsCounterConfig = config.get[NGramsCounterConfig]("ngrams-counter").value

    lazy val nGram1Tool = wireWith(NGramsCounter.factoryNGramsCounter1 _)

    lazy val nGram2Tool = wireWith(NGramsCounter.factoryNGramsCounter2 _)

    lazy val nGram3Tool = wireWith(NGramsCounter.factoryNGramsCounter3 _)

    lazy val phraseCorrelationRepeatedConfig = config.get[PhraseCorrelationRepeatedConfig]("phrase-correlation-repeated").value

    lazy val phraseCorrelationRepeatedTool = wire[PhraseCorrelationRepeated]

    lazy val phraseCorrelationConsequentConfig = config.get[PhraseCorrelationConsequentWithHintsConfig]("phrase-correlation-consequent").value

    lazy val phraseCorrelationConsequentTool = wire[PhraseCorrelationConsequentWithHints]

    lazy val phraseCorrelationInnerWithHintsConfig = config.get[PhraseCorrelationInnerWithHintsConfig]("phrase-correlation-inner-with-hints").value

    lazy val phraseCorrelationInnerTool = wire[PhraseCorrelationInnerWithHints]
  }

  trait MainTools {
    private implicit val ordering = Ordering.
      fromLessThan((x: List[Int], y: List[Int]) => (x zip y).find(x => x._1 != x._2).map(x => x._1 < x._2).getOrElse(false))

    def save(file: File, vocabulary: VocabularyRaw) = {
      val output = new ObjectOutputStream(new FileOutputStream(file))
      output.writeObject(vocabulary)
      output.close()
    }

    def save(file: File, vocabulary: VocabularyImplStored) = {
      val output = new ObjectOutputStream(new FileOutputStream(file))
      output.writeObject(vocabulary)
      output.close()
    }

    def load(file: File): VocabularyRaw = {
      val input = new ObjectInputStream(new FileInputStream(file))
      val vocabulary = input.readObject()
      input.close()
      vocabulary.asInstanceOf[VocabularyRaw]
    }

    def dump(ngram1: Map[List[Token], Int]) = {
      ngram1.toList.sortBy(_._1).foreach {
        case (key, value) =>
          println(s" ${key.map(_.toString).mkString("", " :: ", " :: Nil")} -> $value")
      }
    }

    def dump(phrases: Seq[List[Token]]) =
      phrases foreach { statement =>
        println(statement.mkString("", " :: ", " :: Nil"))
      }


    def dump(toToken: Map[Word, List[Token]], lastToken: Int) = {
      println(s"Last token = $lastToken")
      toToken.
        toList.
        sortBy(_._1).
        foreach {
          case (key, value :: _) =>
            println(f"$key%-60s:$value%010d")
        }
    }

    def dump(tokenHist: Map[Token, Int], token2Word: Map[Token, Word]) = {
      tokenHist.
        toList.
        flatMap {
          case (key, count) =>
            token2Word.get(key) map {
              _ -> count
            }
        }.
        sortBy(_._2).
        reverse.
        foreach {
          case (key, value) =>
            println(f" $key%-60s -> $value%010d")
        }
    }
  }

}
