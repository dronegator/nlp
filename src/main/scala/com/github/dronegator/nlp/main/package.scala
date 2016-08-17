package com.github.dronegator.nlp

import java.io.File

import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.ngramscounter.NGramsCounter
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.splitter.Splitter
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.vocabulary.VocabularyRaw

/**
 * Created by cray on 8/17/16.
 */
package object main {
   trait Combinators {
     def cfg: CFG

     lazy val splitter = new Splitter(cfg)

     lazy val tokenizer = new Tokenizer(cfg, None)

     lazy val phraseDetector = new PhraseDetector(cfg)

     lazy val accumulator = new Accumulator(cfg, phraseDetector)

     lazy val ngramms1 = new NGramsCounter(cfg, 1)

     lazy val ngramms2 = new NGramsCounter(cfg, 2)

     lazy val ngramms3 = new NGramsCounter(cfg, 3)
   }

   trait MainTools extends Combinators {

     private implicit val ordering = Ordering.
       fromLessThan((x: List[Int], y: List[Int]) => (x zip y).find(x => x._1 != x._2).map(x => x._1 < x._2).getOrElse(false))


     def save(file: File, vocabulary: VocabularyRaw) = ???

     def load(file: File, vocabulary: VocabularyRaw): VocabularyRaw = ???

     def dump(ngrams1: Map[List[Token], Int]) = {
       ngrams1.toList.sortBy(_._1).foreach {
         case (key, value) =>
           println(s" ${key.map(_.toString).mkString("", " :: ", " :: Nil")} -> $value")
       }
     }

     def dump(phrases: Seq[List[Token]]) =
       phrases foreach { phrase =>
         println(phrase.mkString("", " :: ", " :: Nil"))
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

   }
}
