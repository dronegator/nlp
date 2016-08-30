package com.github.dronegator.nlp

import java.io._
import java.nio.file.Paths

import com.github.dronegator.nlp.common.{Count, Probability}
import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.ngramscounter.NGramsCounter
import com.github.dronegator.nlp.component.phrase_correlation_consequent.PhraseCorrelationConsequent
import com.github.dronegator.nlp.component.phrase_correlation_repeated.{PhraseCorrelationInner, PhraseCorrelationRepeated}
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.splitter.Splitter
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.utils.concurrent.Zukunft
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyRaw}
import com.softwaremill.macwire._

import scala.concurrent.Future

/**
 * Created by cray on 8/17/16.
 */
package object main {

  trait Combinators {
    def cfg: CFG

    lazy val splitterTool = wire[Splitter]

    lazy val tokenizerTool = wire[Tokenizer]

    lazy val phraseDetectorTool = wire[PhraseDetector]

    lazy val accumulatorTool = wire[Accumulator]

    lazy val nGram1Tool = wireWith(NGramsCounter.factoryNGramsCounter1 _)

    lazy val nGram2Tool = wireWith(NGramsCounter.factoryNGramsCounter2 _)

    lazy val nGram3Tool = wireWith(NGramsCounter.factoryNGramsCounter3 _)

    lazy val phraseCorrelationRepeatedTool = wire[PhraseCorrelationRepeated]

    lazy val phraseCorrelationConsequentTool = wire[PhraseCorrelationConsequent]

    lazy val phraseCorrelationInnerTool = wire[PhraseCorrelationInner]
  }

  trait MainTools extends Combinators {
    private implicit val ordering = Ordering.
      fromLessThan((x: List[Int], y: List[Int]) => (x zip y).find(x => x._1 != x._2).map(x => x._1 < x._2).getOrElse(false))

    def save(file: File, vocabulary: VocabularyRaw) = {
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
