package com.github.dronegator.nlp.main

import java.io.File

import akka.stream.scaladsl.Source
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.utils.{Match, CFG}
import com.github.dronegator.nlp.vocabulary.VocabularyTools.VocabularyTools
import com.github.dronegator.nlp.vocabulary.VocabularyTools.VocabularyRawTools
import com.github.dronegator.nlp.vocabulary.VocabularyImpl
import enumeratum.EnumEntry.Lowercase
import enumeratum._

import scala.collection.immutable.::

/**
 * Created by cray on 8/17/16.
 */

object NLPTReplMain
  extends App
  with MainTools
  with Concurent
  with DumpTools {
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

  object OptFile extends Match[List[String], Option[File]]({
    case Nil => None
    case file :: Nil => Some(new File(file))
  })

  object File1 extends Match[String, File]({
    case file => new File(file)
  })

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

    case object Probability extends Command("Calculate a probability of the statement", Set(Dump))

    case object Lookup extends Command("Show probability of n-gram", Set())

    case object Continue extends Command("Show possible continuation of n-gram", Set())

    case object ContinuePhrase extends Command("Show possible continuation of a statement", Set())

    case object Everything extends Command("Statistic for all items of a vocabulary", Set(Dump, Stat))

    case object Advice extends Command("Provide an advice to improve the statement", Set())

    case object Generate extends Command("Generate a statement from a word", Set())

    case object Meaning extends Command("Evaluate meaning of the words", Set())

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
      exec(s.split("\\s+").filter(_.trim.nonEmpty).toList);
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

    case NGram1() :: Dump() :: OptFile(file)  =>
      println("== ngram1")
      sourceFromCount(vocabulary.nGram1.toIterator).arbeiten(file)

    case NGram1() :: _ =>
      println(s"== ngram1 size = ${vocabulary.nGram1.size}")

    case NGram2() :: Dump() :: OptFile(file) =>
      println("== ngram2")
      sourceFromCount(vocabulary.nGram2.toIterator).arbeiten(file)

    case NGram2() :: _ =>
      println(s"== ngram2 size = ${vocabulary.nGram2.size}")

    case NGram3() :: Dump() :: OptFile(file) =>
      println("== ngram3")
      sourceFromCount(vocabulary.nGram3.toIterator).arbeiten(file)

    case NGram3() :: _ =>
      println(s"== ngram3 size = ${vocabulary.nGram3.size}")

    case Phrases() :: Dump() :: OptFile(file) =>
      println("== phrases")
      sourceFrom(vocabulary.phrases.toIterator map { x =>
        x -> vocabulary.probability(x)
      }).arbeiten(file)

    case Phrases() :: _ =>
      println(s"== phrases size = ${vocabulary.phrases.size}")

    case Tokens() :: Dump() :: OptFile(file) =>
      println("== tokens")
      sourceFromTokenMap(vocabulary.tokenMap.toIterator).arbeiten(file)

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

    case Next() :: Dump() :: _ =>
      println("== next")
      println(vocabulary.map1ToNextPhrase.keys.toList.flatten.flatMap(vocabulary.wordMap.get(_)))

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

    case Probability() :: Dump() :: OptFile(file) =>
      SourceExt(Source(vocabulary.phrases).map { tokens =>
        val probability = vocabulary.probability(tokens)
        val statement = vocabulary.untokenize(tokens)
        (f"${tokens.length}%-3d ${probability}%-16.14f $statement")
      }).arbeiten(file)

    case Probability() :: words =>
      val statement = vocabulary.tokenize(words)
      val probability = vocabulary.probability(statement)

      println(
        f"""
           | probability = ${probability}%16.14f
           | length = ${statement.length}
           | tokens = ${statement.mkString("", " :: ", " :: Nil")}
         """.stripMargin)

    case Generate() :: words =>
      vocabulary.generatePhrase(vocabulary.tokenizeShort(words)).
        foreach {
          case tokens =>
            println(vocabulary.untokenize(tokens))
        }

    case com@Advice() :: words =>
      vocabulary.advicePlain(vocabulary.tokenize(words)).
        foreach {
          case (statements, n) if !statements.isEmpty =>
            statements.foreach {
              case (statement, d) =>
                val phrase = statement.flatMap(vocabulary.wordMap.get(_)).mkString(" ")
                println(f"$d%5.4f $phrase")
            }

          case _ =>
        }


    case Meaning() :: File1(sense) :: File1(nonSense) :: OptFile(weighted) =>
      val meaningMap = vocabulary.meaningContextMap(
        io.Source.fromFile(sense).getLines().flatMap(vocabulary.tokenMap.get(_)).flatMap(_.headOption).toList,
        io.Source.fromFile(nonSense).getLines().flatMap(vocabulary.tokenMap.get(_)).flatMap(_.headOption).toList
      )

      Source.fromIterator( () => vocabulary.meaningWordMap(meaningMap).iterator ).map{
        case (token, (sense, nonsense, common)) =>
          vocabulary.wordMap.get(token).map{ word =>
            f"$word $sense%14.12f $nonsense%14.12f $common%14.12f"
          }
      }.
      collect{
        case Some(x) => x
      }.arbeiten(weighted)



    //case ContinuePhrase() :: words =>
    case words@(_ :+ ".") =>
      println("We suggest a few words for the next phrase:")
      vocabulary.suggestForNext(vocabulary.tokenize(words)).
        flatMap {
          case (token, p) =>
            vocabulary.wordMap.get(token).map(_ -> p)
        }.
        foreach {
          case (word, p) =>
            println(s" - $word, p = $p")
        }

    case Continue() :: words =>
      vocabulary.continueStatement(vocabulary.tokenizeShort(words)) foreach {
        case (token, probability) =>
          println(s" - ${RepresenterToken.represent(token)}, p = $probability")
      }

    case words@(_ :: _) =>
      vocabulary.continueStatement(vocabulary.tokenizeShort(words)) foreach {
        case (token, probability) =>
          println(s" - ${RepresenterToken.represent(token)}, p = $probability")
      }

    case _ =>
      println(com.github.dronegator.nlp.main.Version.versionMessageExtended
      )
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

  override def tokenToDump(token: Token): String =
    tokenToWordString(token)
//      tokenToString(token)
}
