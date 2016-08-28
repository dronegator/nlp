package com.github.dronegator.nlp.main

import java.io.File
import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.ByteString
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenPreDef}
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.utils.RandomUtils._
import com.github.dronegator.nlp.utils.concurrent.Zukunft
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import enumeratum.EnumEntry.Lowercase
import enumeratum._

/**
 * Created by cray on 8/17/16.
 */

object NLPTReplMain
  extends App
  with MainTools {
  lazy val cfg: CFG = CFG()

  implicit val context = scala.concurrent.ExecutionContext.global

  implicit val system = ActorSystem()

  implicit val mat = ActorMaterializer()

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

    case object Probability extends Command("Calculate a probability of the phrase", Set(Dump))

    case object Lookup extends Command("Show probability of n-gram", Set())

    case object Continue extends Command("Show possible continuation of n-gram", Set())

    case object ContinuePhrase extends Command("Show possible continuation of a phrase", Set())

    case object Everything extends Command("Statistic for all items of a vocabulary", Set(Dump, Stat))

    case object Advice extends Command("Provide an advice to improve the phrase", Set())

    case object Generate extends Command("Generate a phrase from a word", Set())

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
      dump(vocabulary.nGram1)
      println("==")

    case NGram2() :: Dump() :: _ =>
      println("== ngram2")
      dump(vocabulary.nGram2)
      println("==")

    case NGram3() :: Dump() :: _ =>
      println("== ngram3")
      dump(vocabulary.nGram3)
      println("==")

    case Phrases() :: Dump() :: _ =>
      println("== phrases")
      dump(vocabulary.phrases)
      println("==")

    case Next() :: Dump() :: _ =>
      println("== next")
      //println(vocabulary.vcnext.keys)
      println(vocabulary.map1ToNextPhrase.keys.toList.flatten.flatMap(vocabulary.wordMap.get(_)))
      println("==")

    case Tokens() :: Dump() :: _ =>
      println("== tokens")
      dump(vocabulary.tokenMap, vocabulary.tokenMap.size)
      println("==")

    case NGram1() :: _ =>
      println(s"== ngram1 size = ${vocabulary.nGram1.size}")

    case NGram2() :: _ =>
      println(s"== ngram2 size = ${vocabulary.nGram2.size}")

    case NGram3() :: _ =>
      println(s"== ngram3 size = ${vocabulary.nGram3.size}")

    case Phrases() :: _ =>
      println(s"== phrases size = ${vocabulary.phrases.size}")

    case Tokens() :: _ =>
      println(s"== tokens size = ${vocabulary.tokenMap.size}")

    case Everything() :: _ =>
      println(
        s"""
           |Statistic:
           | - ngram1 size = ${vocabulary.nGram1.size}
           | - ngram2 size = ${vocabulary.nGram2.size}
           | - ngram3 size = ${vocabulary.nGram3.size}
           | - phrases size = ${vocabulary.phrases.size}
           | - tokens size = ${vocabulary.tokenMap.size}
         """.stripMargin)

    case Probability() :: Dump() :: (file@(_ :: Nil | Nil)) =>
      val probabilities = Source(vocabulary.phrases).map { tokens =>
        val probability = vocabulary.probability(tokens)
        val phrase = vocabulary.untokenize(tokens)
        (f"${tokens.length}%-3d ${probability}%-16.14f $phrase")
      }

      file.headOption match {
        case Some(file) =>
          probabilities.map(x => ByteString(x + "\n")).runWith(FileIO.toPath(Paths.get(file))).foreach { _ =>
            println(s"Dumping to $file finished")
          }

        case None =>
          probabilities.runForeach {
            println(_)
          }.await
      }

    case Probability() :: args =>
      val phrase = args.mkString(" ")
      val tokens = vocabulary.tokenize(phrase)
      val probability = vocabulary.probability(tokens)

      println(
        f"""
           | probability = ${probability}%16.14f
           | length = ${tokens.length}
           | tokens = ${tokens.mkString("", " :: ", " :: Nil")}
         """.stripMargin)

    case Lookup() :: word1 :: Nil =>
      println(s"1 $word1:")
      for {
        token1 <- vocabulary.tokenMap(word1)
        x <- vocabulary.pToken.get(token1 :: Nil)
      } {
        println(s" - $token1 => $x")
      }

    case Lookup() :: word1 :: word2 :: Nil =>
      println(s"2 $word1 $word2:")
      for {
        token1 <- vocabulary.tokenMap(word1)
        token2 <- vocabulary.tokenMap(word2)
        x <- vocabulary.pNGram2.get(token1 :: token2 :: Nil)
      } {
        println(s" - $token1 $token2 => $x")
      }
    case Lookup() :: word1 :: word2 :: word3 :: _ =>
      println(s"3 $word1 $word2 $word3:")
      for {
        token1 <- vocabulary.tokenMap(word1)
        token2 <- vocabulary.tokenMap(word2)
        token3 <- vocabulary.tokenMap(word3)
        x <- vocabulary.pNGram3.get(token1 :: token2 :: token3 :: Nil)
      } {
        println(s" - $token1 $token2 $token3 => $x")
      }

    case Continue() :: word1 :: Nil =>
      println(s"$word1:")
      for {
        token1 <- vocabulary.tokenMap(word1)
        (p, nextToken) <- vocabulary.map1ToNext(token1 :: Nil)
        nextWord <- vocabulary.wordMap.get(nextToken)
      } {
        println(s" - $nextWord ($nextToken), p = $p")
      }

    case Continue() :: word1 :: word2 :: Nil =>
      println(s"$word1 $word2:")
      for {
        token1 <- vocabulary.tokenMap(word1)
        token2 <- vocabulary.tokenMap(word2)
        (p, nextToken) <- vocabulary.map2ToNext(token1 :: token2 :: Nil)
        nextWord <- vocabulary.wordMap.get(nextToken)
      } {
        println(s" - $nextWord ($nextToken), p = $p")
      }

    case Generate() :: words =>
      val tokens = vocabulary.tokenize(words.mkString(" ")).drop(2).dropRight(1)

      val phrase = Iterator.
        iterate(tokens) {
          case tokens@_ :: Nil =>
            tokens :+ vocabulary.map1ToNext(tokens).
              choiceOption.getOrElse(TokenPreDef.PEnd.value)

          case tokens =>
            tokens :+ vocabulary.map2ToNext(tokens.takeRight(2)).choiceOption.getOrElse(TokenPreDef.PEnd.value)
        }.
        takeWhile(x => !(x.lastOption contains TokenPreDef.PEnd.value)).
        take(20).
        toList.
        lastOption.
        flatMap { tokens =>
          Iterator.
            iterate(tokens) {
              case tokens@_ :: Nil =>
                vocabulary.map1ToPrev(tokens).
                  choiceOption.getOrElse(TokenPreDef.PStart.value) :: tokens

              case tokens@x :: y :: _ =>
                vocabulary.map2ToPrev(x :: y :: Nil).choiceOption.getOrElse(TokenPreDef.PStart.value) :: tokens

            }.
            takeWhile(x => !(x.headOption contains TokenPreDef.PStart.value)).
            take(20).
            toList.
            lastOption
        }

      phrase.
        foreach {
          case tokens =>
            println(vocabulary.untokenize(tokens))
        }

    case com@Advice() :: words =>
      val phrase = vocabulary.tokenize(words.mkString(" "))

      phrase.
        sliding(3).
        zipWithIndex.
        collect {
          case (x :: y :: z :: Nil, n) =>
            (x, y, z, n, phrase)
        }.
        map {
          case (x, y, z, n, phrase) =>

            val (start, token :: end) = phrase.splitAt(n + 1)

            vocabulary.map2ToMiddle.get(x :: z :: Nil).
              toList.
              flatten.
              takeWhile(_._2 != token).
              take(4).
              map {
                case (d, advice) =>
                  (d, start ++ (advice :: end))
              } -> n
        }.
        foreach {
          case (phrases, n) if !phrases.isEmpty =>
            phrases.foreach {
              case (d, tokens) =>
                val phrase = tokens.flatMap(vocabulary.wordMap.get(_)).mkString(" ")
                println(f"$d%5.4f $phrase")
            }

          case _ =>
        }

    //case ContinuePhrase() :: words =>
    case words@(_ :+ ".") =>
      println("We suggest a few words for the next phrase:")
      val advice = (for {
        (token1, _) <-
        vocabulary.filter(
          words.flatMap(vocabulary.tokenMap.get(_)).flatten,
          2, 10).
          toList.
          flatMap(_._2)
        (p, nextToken) <- vocabulary.map1ToNextPhrase.get(token1 :: Nil).toList.flatten
      } yield {
          nextToken -> p
        }).

        foldLeft(Map[Token, Double]()) {
          case (map, (token, p)) =>
            map + (token -> (p + map.getOrElse(token, 0.0)))
        }.
        toList

      vocabulary.filter1(advice.toMap, 2, 10).
        map(_._2).
        toList.
        flatten.
        sortBy(_._2).
        flatMap {
          case (token, p) =>
            vocabulary.wordMap.
              get(token).
              map(_ -> p)
        }.
        foreach {
          case (word, p) =>
            println(s" - $word, p = $p")
        }

    case /*Continue() ::*/ words@(_ :: _) =>
      words.takeRight(2) match {
        case word1 :: word2 :: Nil =>
          println(s"$word1 $word2:")
          for {
            token1 <- vocabulary.tokenMap(word1)
            token2 <- vocabulary.tokenMap(word2)
            (p, nextToken) <- vocabulary.map2ToNext.get(token1 :: token2 :: Nil) orElse vocabulary.map1ToNext.get(token1 :: Nil) getOrElse List()
            nextWord <- vocabulary.wordMap.get(nextToken)
          } {
            println(s" - $nextWord ($nextToken), p = $p")
          }

        case word1 :: Nil =>
          println(s"$word1:")
          for {
            token1 <- vocabulary.tokenMap(word1)
            (p, nextToken) <- vocabulary.map1ToNext(token1 :: Nil)
            nextWord <- vocabulary.wordMap.get(nextToken)
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
