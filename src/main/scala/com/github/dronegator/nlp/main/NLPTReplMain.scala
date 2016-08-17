package com.github.dronegator.nlp.main

import java.io.File

import com.github.dronegator.nlp.utils.CFG
import enumeratum.EnumEntry.Lowercase
import enumeratum._

/**
 * Created by cray on 8/17/16.
 */

object NLPTReplMain
  extends App
  with MainTools {
  lazy val cfg: CFG = CFG()

  sealed trait Command extends EnumEntry with Lowercase with Command.EnumApply

  object Command extends Enum[Command] {
    override def values: Seq[Command] = findValues

    trait EnumApply {
      def unapply(name: String) = withNameOption(name) filter ( x => x == this) isDefined
    }

    case object NGram1 extends Command

    case object NGram2 extends Command

    case object NGram3 extends Command

    case object Phrases extends Command

    case object Tokens extends Command
    
    case object Probability extends Command

    case object Everything extends Command

    def unapply(name: String) = withNameOption(name)
  }

  sealed trait SubCommand extends EnumEntry with Lowercase with SubCommand.EnumUnaply

  object SubCommand extends Enum[SubCommand] {
    override def values: Seq[SubCommand] = findValues

    trait EnumUnaply {
      def unapply(name: String) = withNameOption(name) filter (this == _) isDefined
    }

    case object Dump extends SubCommand

    case object Stat extends SubCommand

    def unapply(name: String) = withNameOption(name)
  }

  import SubCommand._
  import Command._

  val Array(fileIn) = args

  lazy val vocabulary = load(new File(fileIn))

  val ConsoleReader = new jline.console.ConsoleReader()

  def task(): Unit = ConsoleReader.readLine("> ") match {
    case s : String => try {
      exec(s.split("\\s+").toList);
    } catch {
      case x  => printf("\n* Command failure: %s\n\n",x)
    } finally task()
    case _ => {}
  }

  def exec(args: List[String]) = args match {
    case NGram1() :: Dump() :: _ =>
      println("== ngram1")
      dump(vocabulary.ngrams1)
      println("==")

    case NGram2() :: Dump() :: _ =>
      println("== ngram2")
      dump(vocabulary.ngrams2)
      println("==")

    case NGram3() :: Dump() :: _ =>
      println("== ngram3")
      dump(vocabulary.ngrams3)
      println("==")

    case Phrases() :: Dump() :: _ =>
      println("== phrases")
      dump(vocabulary.phrases)
      println("==")

    case Tokens() :: Dump() :: _ =>
      println("== tokens")
      dump(vocabulary.toToken, vocabulary.toToken.size)
      println("==")

    case NGram1() :: _ =>
      println(s"== ngram1 size = ${vocabulary.ngrams1.size}")

    case NGram2() :: _ =>
      println(s"== ngram2 size = ${vocabulary.ngrams2.size}")

    case NGram3() :: _ =>
      println(s"== ngram3 size = ${vocabulary.ngrams3.size}")

    case Phrases() :: _ =>
      println(s"== phrases size = ${vocabulary.phrases.size}")

    case Tokens() :: _ =>
      println(s"== tokens size = ${vocabulary.toToken.size}")

    case Everything() :: _ =>
      println(
        s"""
           |Statistic:
           | - ngram1 size = ${vocabulary.ngrams1.size}
           | - ngram2 size = ${vocabulary.ngrams2.size}
           | - ngram3 size = ${vocabulary.ngrams3.size}
           | - phrases size = ${vocabulary.phrases.size}
           | - tokens size = ${vocabulary.toToken.size}
         """.stripMargin)

    case Probability() :: args =>
      val phrase = args.mkString(" ")
      println(s"== phrase = ${phrase}")

    case _ =>
      Command.values foreach { value =>
        println(value)
      }
  }

  try {
    task()
  } finally {
    println("end")
    ConsoleReader.clearScreen()
    ConsoleReader.shutdown()
  }
}
