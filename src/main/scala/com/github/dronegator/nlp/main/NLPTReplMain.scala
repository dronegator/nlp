package com.github.dronegator.nlp.main

import java.io.File

import com.github.dronegator.nlp.component.accumulator.Accumulator
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenPreDef}
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import enumeratum.EnumEntry.Lowercase
import enumeratum._
import org.omg.CosNaming.NamingContextPackage.NotEmpty

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
      def unapply(name: String) = withNameOption(name) filter (x => x == this) isDefined
    }

    case object NGram1 extends Command("Dump or Stat 1-gram sequences in a vocabulary", Set(Dump, Stat))

    case object NGram2 extends Command("Dump or Stat 1-gram sequences in a vocabulary", Set(Dump, Stat))

    case object NGram3 extends Command("Dump or Stat 1-gram sequences in a vocabulary", Set(Dump, Stat))

    case object Phrases extends Command("Dump or Stat phrases in a vocabulary", Set(Dump, Stat))

    case object Tokens extends Command("Dump or Stat tokens sequences in a vocabulary", Set(Dump, Stat))

    case object Next extends Command("Dump or Stat correlation map for phrases", Set(Dump, Stat))

    case object Probability extends Command("Calculate a probability of the phrase", Set())

    case object Lookup extends Command("Show probability of n-gram", Set())

    case object Continue extends Command("Show possible continuation of n-gram", Set())

    case object ContinuePhrase extends Command("Show possible continuation of a phrase", Set())

    case object Everything extends Command("Statistic for all items of a vocabulary", Set(Dump, Stat))

    case object Advice extends Command("Provide an advice to improve the phrase", Set())

    def unapply(name: String): Option[(Command, String, Set[SubCommand])] = withNameOption(name) map {
      case Command(x, y, z) => (x, y, z)
    }

    def unapply(x: Command) =
      Some((x, x.help, x.subcommands))
  }

  import Command._

  val Array(fileIn) = args

  lazy val vocabulary: VocabularyImpl = load(new File(fileIn))

  val ConsoleReader = new jline.console.ConsoleReader()

  def task(): Unit = ConsoleReader.readLine("> ") match {
    case s: String => try {
      exec(s.split("\\s+").toList);
    } catch {
      case th: Throwable =>
        printf("\n* Command failure: %s\n\n", th)
        th.printStackTrace()
    } finally task()
    case _ => {}
  }

  def exec(args: List[String]) = args match {
    case Command(command, help, subcommands) :: Nil =>
      println(
        s"""
           | >> ${command} ${subcommands mkString("[", " | ", "]")}
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

    case Next() :: Dump() :: _ =>
      println("== next")
      //println(vocabulary.vcnext.keys)
      println(vocabulary.vcnext.keys.toList.flatten.flatMap(vocabulary.toWord.get(_)))
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

    case Lookup() :: word1 :: Nil =>
      println(s"1 $word1:")
      for {
        token1 <- vocabulary.toToken(word1)
        x <- vocabulary.vngrams1.get(token1 :: Nil)
      } {
        println(s" - $token1 => $x")
      }

    case Lookup() :: word1 :: word2 :: Nil =>
      println(s"2 $word1 $word2:")
      for {
        token1 <- vocabulary.toToken(word1)
        token2 <- vocabulary.toToken(word2)
        x <- vocabulary.vngrams2.get(token1 :: token2 :: Nil)
      } {
        println(s" - $token1 $token2 => $x")
      }
    case Lookup() :: word1 :: word2 :: word3 :: _ =>
      println(s"3 $word1 $word2 $word3:")
      for {
        token1 <- vocabulary.toToken(word1)
        token2 <- vocabulary.toToken(word2)
        token3 <- vocabulary.toToken(word3)
        x <- vocabulary.vngrams3.get(token1 :: token2 :: token3 :: Nil)
      } {
        println(s" - $token1 $token2 $token3 => $x")
      }

    case Continue() :: word1 :: Nil =>
      println(s"$word1:")
      for {
        token1 <- vocabulary.toToken(word1)
        (p, nextToken) <- vocabulary.vnext1(token1 :: Nil)
        nextWord <- vocabulary.toWord.get(nextToken)
      } {
        println(s" - $nextWord ($nextToken), p = $p")
      }

    case Continue() :: word1 :: word2 :: Nil =>
      println(s"$word1 $word2:")
      for {
        token1 <- vocabulary.toToken(word1)
        token2 <- vocabulary.toToken(word2)
        (p, nextToken) <- vocabulary.vnext2(token1 :: token2 :: Nil)
        nextWord <- vocabulary.toWord.get(nextToken)
      } {
        println(s" - $nextWord ($nextToken), p = $p")
      }

    //case ContinuePhrase() :: words =>
    case words if words.lastOption.contains(".") =>
      (for {
        (token1, _) <-
        vocabulary.filter(
          words.flatMap(vocabulary.toToken.get(_)).flatten,
          2).
          toList.
          flatMap(_._2)
        (p, nextToken) <- vocabulary.vcnext.get(token1 :: Nil).toList.flatten
      } yield {
          nextToken -> p
        }).
        foldLeft(Map[Token, Double]()) {
          case (map, (token, p)) =>
            map + (token -> (p + map.getOrElse(token, 0.0)))
        }.
        flatMap {
          case (token, p) =>
            vocabulary.toWord.
              get(token).
              map(_ -> p)
        }.
        toList.
        sortBy(_._2).
        foreach {
          case (word, p) =>
            println(s" - $word, p = $p")
        }

    case Advice() :: words =>
      val tokens = splitter(words.mkString(" ")).
//        map{ x =>
//          println(x)
//          x
//        }.
        scanLeft((vocabulary.toToken, 100000000, Tokenizer.Init._3))(tokenizer(_, _)).
        map {
          case (_, _, tokens) => tokens
        }.
        toList :+ List(TokenPreDef.TEnd.value)

      val phrase = tokens.
        scanLeft(Accumulator.Init)(accumulator(_, _)).
        collect {
          case (_, Some(phrase)) => phrase
        }.
        flatten

      phrase.
        sliding(3).
        zipWithIndex.
        collect{
          case (x :: y :: z :: Nil, n) =>
            (x, y, z, n, phrase)
        }.
        map{
          case (x, y, z, n, phrase) =>

            val (start, token :: end) = phrase.splitAt(n+1)

            //println(start.flatMap(vocabulary.toWord.get(_)).mkString(" "), token, end.flatMap(vocabulary.toWord.get(_)).mkString(" "))

            vocabulary.vmiddle.get(x :: z :: Nil).
              toList.
              flatten.
              takeWhile(_._2 != token).
              take(4).
              map{
                case (d, advice) =>
                  (d, start ++ (advice :: end))
              } -> n
        }.
        foreach{
          case (phrases, n) if !phrases.isEmpty =>
            println(s" == $n ==")
            phrases.foreach{
              case (d, tokens) =>
                val phrase = tokens.flatMap(vocabulary.toWord.get(_)).mkString(" ")
                println(f"$d%5.4f $phrase")
            }

          case _ =>
        }

    case /*Continue() ::*/ words@(_ :: _) =>
      words.takeRight(2) match {
        case word1 :: word2 :: Nil =>
          println(s"$word1 $word2:")
          for {
            token1 <- vocabulary.toToken(word1)
            token2 <- vocabulary.toToken(word2)
            (p, nextToken) <- vocabulary.vnext2.get(token1 :: token2 :: Nil) orElse vocabulary.vnext1.get(token1 :: Nil) getOrElse List()
            nextWord <- vocabulary.toWord.get(nextToken)
          } {
            println(s" - $nextWord ($nextToken), p = $p")
          }

        case word1 :: Nil =>
          println(s"$word1:")
          for {
            token1 <- vocabulary.toToken(word1)
            (p, nextToken) <- vocabulary.vnext1(token1 :: Nil)
            nextWord <- vocabulary.toWord.get(nextToken)
          } {
            println(s" - $nextWord ($nextToken), p = $p")
          }
      }

    case _ =>
      Command.values foreach {
        case Command(command, help, subcommands) =>
          println(
            s""" >> ${command} ${subcommands mkString("[", " | ", "]")}
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
