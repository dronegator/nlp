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

  sealed trait Command extends EnumEntry with Lowercase

  object Command extends Enum[Command] {
    override def values: Seq[Command] = findValues

    case object NGram1 extends Command

    case object NGram2 extends Command

    case object NGram3 extends Command

    case object Phrases extends Command

    case object Tokens extends Command

    def unapply(name: String) = withNameOption(name)
  }

  sealed trait SubCommand extends EnumEntry with Lowercase

  object SubCommand extends Enum[SubCommand] {
    override def values: Seq[SubCommand] = findValues

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
      case x  => printf("\n* Comand failure: %s\n\n",x)
    } finally task()
    case _ => {}
  }

  def exec(args: List[String]) = args match {
    case Command(NGram1) :: SubCommand(Dump) :: _ =>
      println("== ngram1")
      dump(vocabulary.ngrams1)
      println("==")

    case Command(NGram2) :: SubCommand(Dump) :: _ =>
      println("== ngram1")
      dump(vocabulary.ngrams2)
      println("==")

    case Command(NGram3) :: SubCommand(Dump) :: _ =>
      println("== ngram1")
      dump(vocabulary.ngrams3)
      println("==")

    case Command(Phrases) :: SubCommand(Dump) :: _ =>
      println("== phrases")
      dump(vocabulary.phrases)
      println("==")

    case Command(Tokens) :: SubCommand(Dump) :: _ =>
      println("== phrases")
      dump(vocabulary.toToken, vocabulary.toToken.size)
      println("==")

    case Command(NGram1) :: _ =>
      println(s"== ngram1 size = ${vocabulary.ngrams1.size}")

    case Command(NGram2) :: _ =>
      println(s"== ngram1 size = ${vocabulary.ngrams2.size}")

    case Command(NGram3) :: _ =>
      println(s"== ngram1 size = ${vocabulary.ngrams3.size}")

    case Command(Phrases) :: _ =>
      println(s"== phrases size = ${vocabulary.phrases.size}")

    case Command(Tokens) :: SubCommand(Dump) :: _ =>
      println(s"== tokens size = ${vocabulary.toToken.size}")
      println("==")

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
