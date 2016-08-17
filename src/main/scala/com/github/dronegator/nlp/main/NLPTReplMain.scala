package com.github.dronegator.nlp.main

import java.io.File

import com.github.dronegator.nlp.utils.CFG
import enumeratum.EnumEntry.Lowercase
import enumeratum._
import enumeratum.values.{ValueEnum, ValueEnumEntry}

/**
 * Created by cray on 8/17/16.
 */

object NLPTReplMain
  extends App
  with MainTools {
  lazy val cfg: CFG = CFG()

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

  sealed abstract class Command(val help: String, val subcommands: Set[SubCommand]) extends EnumEntry with Lowercase with Command.EnumApply

  object Command extends Enum[Command] {
    override def values: Seq[Command] = findValues

    trait EnumApply {
      def unapply(name: String) = withNameOption(name) filter ( x => x == this) isDefined
    }

    case object NGram1 extends Command("Dump or Stat 1-gram sequences in a vocabulary", Set(Dump, Stat))

    case object NGram2 extends Command("Dump or Stat 1-gram sequences in a vocabulary", Set(Dump, Stat))

    case object NGram3 extends Command("Dump or Stat 1-gram sequences in a vocabulary", Set(Dump, Stat))

    case object Phrases extends Command("Dump or Stat phrases in a vocabulary", Set(Dump, Stat))

    case object Tokens extends Command("Dump or Stat tokens sequences in a vocabulary", Set(Dump, Stat))
    
    case object Probability extends Command("Calculate a probability of the phrase", Set())

    case object Everything extends Command("Statistic for all items of a vocabulary", Set(Dump, Stat))

    def unapply(name: String): Option[(Command, String, Set[SubCommand])] = withNameOption(name) map {
      case Command(x, y, z) => (x, y, z)
    }

    def unapply(x: Command) =
      Some((x, x.help,x.subcommands))
  }

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
    case Command(command, help, subcommands) :: Nil =>
      println(
        s"""
          | >> ${command} ${subcommands mkString ("["," | ", "]")}
          |    $help
        """.stripMargin)

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
      Command.values foreach {
        case Command(command, help, subcommands) =>
          println(s""" >> ${command} ${subcommands mkString ("["," | ", "]")}
                      |    $help
                      |    """.stripMargin)
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
