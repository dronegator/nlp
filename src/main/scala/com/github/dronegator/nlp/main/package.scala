package com.github.dronegator.nlp

import java.io._
import java.nio.file.Paths

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
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

  trait Concurent {
    implicit val context = scala.concurrent.ExecutionContext.global

    implicit val system = ActorSystem()

    implicit val mat = ActorMaterializer()
  }

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

  trait DumpTools {
    this: Concurent =>

    def vocabulary: Vocabulary

    implicit class SourceExt[Mat](source: Source[String, Mat]) /*extends AnyVal*/ {
      def arbeiten(): Unit =
        source.runWith(dumpSink()).await

      def arbeiten(file: File): Unit =
        source.runWith(dumpSink(file))

      def arbeiten(file: Option[File]): Unit =
        file.map(arbeiten(_)).getOrElse(arbeiten())
    }

    def tokenToDump(token: Token): String

    def tokenToString(token: Token) = f"$token"

    def tokenToWordString(token: Token) =
      vocabulary.wordMap.get(token) orElse
        TokenPreDef.withValueOpt(token) getOrElse
        (token, "unknown") toString

    trait Representer[A] {
      def represent(a: A): String
    }

    implicit object RepresenterToken extends Representer[Token] {
      override def represent(token: Token): String =
        tokenToDump(token)
    }

    implicit object RepresenterPhrase extends Representer[Statement] {
      override def represent(statement: Statement): String =
        statement.map(tokenToDump(_)).mkString("", " ", "") //.mkString("", " :: ", " :: Nil")
    }

    def sourceFromTokenMap(data: Iterator[(Word, List[Token])]) = //(implicit representer: Representer[List[Token]]) =
      Source.fromIterator(() => data).
        map {
          case (word, payload) =>
            f"$word%-16s : ${payload.mkString("", " :: ", " :: Nil")}"
        }

    def sourceFrom[A](data: Iterator[(A, Probability)])(implicit representer: Representer[A]) =
      Source.fromIterator(() => data).
        map {
          case (payload, probability) =>
            f"$probability%-16.14f : ${representer.represent(payload)}"
        }

    def sourceFromCount[A](data: Iterator[(A, Count)])(implicit representer: Representer[A]) =
      Source.fromIterator(() => data).
        map {
          case (payload, count) =>
            f"$count%8d : ${representer.represent(payload)}"
        }

    def dumpSink(file: File): Sink[String, Future[Done]] =
      Flow[String].
        map(x => ByteString(x + "\n")).
        toMat(FileIO.toPath(Paths.get(file.toString)))(Keep.right).
        mapMaterializedValue { mat =>
          println(s"Dumping to $file has finished with $mat")
          Future.successful(Done)
        }

    def dumpSink(): Sink[String, Future[Done]] =
      Flow[String].
        toMat(Sink.foreach {
          println(_)
        })(Keep.right).
        mapMaterializedValue { mat =>
          mat map { mat =>
            println(s"Dumping has finished with $mat")
            mat
          }
        }
  }

}
