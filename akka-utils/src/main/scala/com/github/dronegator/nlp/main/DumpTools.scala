package com.github.dronegator.nlp.main

import java.io.File
import java.nio.file.Paths

import akka.Done
import akka.stream.scaladsl._
import akka.util.ByteString
import com.github.dronegator.nlp.common._
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.Vocabulary

import scala.concurrent.Future
import com.github.dronegator.nlp.utils._
import com.github.dronegator.nlp.utils.concurrent.Zukunft

/**
 * Created by cray on 8/30/16.
 */
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
        mat.map{ mat =>
          println(s"Dumping to $file has finished with $mat")
          Done
        }
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
