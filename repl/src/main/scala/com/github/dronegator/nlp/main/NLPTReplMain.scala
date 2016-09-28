package com.github.dronegator.nlp.main

import java.io.File

import akka.stream.scaladsl.{Sink, Source}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{PEnd, PStart}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenPreDef}
import com.github.dronegator.nlp.trace._
import com.github.dronegator.nlp.utils.Match._
import com.github.dronegator.nlp.utils._
import com.github.dronegator.nlp.utils.concurrent.Zukunft
import com.github.dronegator.nlp.vocabulary.ToolMiniLanguage.VocabularyToolsAkka
import com.github.dronegator.nlp.vocabulary.VocabularyTools.{VocabularyHintTools, VocabularyRawTools, VocabularyTools}
import com.github.dronegator.nlp.vocabulary.{Vocabulary, VocabularyHint, VocabularyImpl, VocabularyImplStored}
import enumeratum.EnumEntry.Lowercase
import enumeratum._
import jline.console.completer.StringsCompleter
import jline.console.history.FileHistory

import scala.collection.JavaConverters._

/**
  * Created by cray on 8/17/16.
  */

object NLPTReplMain
  extends App
    with NLPTAppPartial
    with MainTools
    with Concurent
    with DumpTools {
  lazy val cfg: CFG = CFG()

  sealed trait SubCommand extends EnumEntry with Lowercase with SubCommand.Overall

  object SubCommand extends Enum[SubCommand] {
    override def values: Seq[SubCommand] = findValues

    trait Overall {
      def unapply(name: String) = withNameOption(name) filter (this == _) isDefined
    }

    case object Dump extends SubCommand

    case object Stat extends SubCommand

    def unapply(name: String) = withNameOption(name)

    def unapply(names: List[String]) = names match {
      case name :: Nil =>
        withNameOption(name)
      case _ =>
        None
    }
  }

  import SubCommand._

  sealed abstract class Command(val help: String, val subcommands: Set[SubCommand])
    extends EnumEntry
      with Lowercase
      with Command.Extractor

  object Command extends Enum[Command] {
    override def values: Seq[Command] = findValues

    trait Extractor {
      def unapply(name: String) = withNameOption(name) filter (x => x == this) isDefined
    }

    case object NGram1 extends Command("Dump or Stat 1-gram sequences in a vocabulary", Set(Dump, Stat))

    case object NGram2 extends Command("Dump or Stat 1-gram sequences in a vocabulary", Set(Dump, Stat))

    case object NGram3 extends Command("Dump or Stat 1-gram sequences in a vocabulary", Set(Dump, Stat))

    case object Phrases extends Command("Dump or Stat phrases in a vocabulary", Set(Dump, Stat))

    case object Tokens extends Command("Dump or Stat tokens sequences in a vocabulary", Set(Dump, Stat))

    case object Next extends Command("Dump or Stat correlation map for phrases", Set(Dump, Stat))

    case object Inner extends Command("Dump or Stat correlation map for words in phrase", Set(Dump, Stat))

    case object Probability extends Command("Calculate a probability of the statement", Set(Dump))

    case object Lookup extends Command("Show probability of n-gram", Set())

    case object Continue extends Command("Show possible continuation of n-gram", Set())

    case object ContinuePhrase extends Command("Show possible continuation of a statement", Set())

    case object Expand extends Command("Suggest additional words for the same phrase", Set())

    case object Everything extends Command("Statistic for all items of a vocabulary", Set(Dump, Stat))

    case object Advice extends Command("Provide an advice to improve the statement", Set())

    case object Generate extends Command("Generate a statement from a word", Set())

    case object Meaning extends Command("Evaluate meaning of the words", Set())

    case object Keywords extends Command("Select keywords from a phrase", Set())

    case object Store extends Command("Store indexes to a file", Set())

    case object MiniLanguage extends Command("Try Mini Language tool", Set())

    def unapply(name: String): Option[(Command, String, Set[SubCommand])] = withNameOption(name) map {
      case Command(x, y, z) => (x, y, z)
    }

    def unapply(x: Command) =
      Some((x, x.help, x.subcommands))
  }

  import Command._

  val fileIn :: _ = args.toList

  lazy val vocabulary: Vocabulary = load(new File(fileIn)) match {
    case vocabulary: Vocabulary =>
      vocabulary
    case vocabulary =>
      vocabulary: VocabularyImpl
  }

  override def vocabularyHint: VocabularyHint = vocabulary

  val consoleReader = new jline.console.ConsoleReader()
  val history = new FileHistory(new File(".wordmetrix_repl_history").getAbsoluteFile)

  consoleReader.setHistory(history)

  consoleReader.addCompleter(new StringsCompleter(Command.values.map(_.entryName).asJava))

  Runtime.getRuntime().addShutdownHook(new Thread() {
    override def run() {
      println("We hope to see you again")
      consoleReader.getHistory.asInstanceOf[FileHistory].flush()
    }
  })

  def task(): Unit = consoleReader.readLine("> ") match {
    case s: String => try {
      exec(s.split("\\s+").filter(_.trim.nonEmpty).toList);
      consoleReader.getHistory.asInstanceOf[FileHistory].flush()
    } catch {
      case th: Throwable =>
        printf("\n* Command failure: %s\n\n", th)
        th.printStackTrace()
    } finally task()
    case _ => {}
  }

  def exec(args: List[String]) = args match {
    case Store() :: OptFile(file) =>
      println("== Indexes are being stored")
      save(file getOrElse new File(fileIn), VocabularyImplStored(vocabulary))
        .time { t =>
          logger.info(s"Vocabulary has stored in time = $t ")
        }

    case Command(command, help, subcommands) :: Nil =>
      println(
        s"""
           | >> ${command} ${subcommands mkString("[", " | ", "]")}
           |    $help
        """.stripMargin)

    case NGram1() :: Dump() :: OptFile(file) =>
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
      sourceFrom(vocabulary.statements.toIterator map { x =>
        x -> vocabulary.probability(x)
      }).arbeiten(file)

    case Phrases() :: _ =>
      println(s"== phrases size = ${vocabulary.statements.size}")

    case Tokens() :: Dump() :: OptFile(file) =>
      println("== tokens")
      sourceFromTokenMap(vocabulary.tokenMap.toIterator).arbeiten(file)

    case Tokens() :: _ =>
      println(s"== tokens size = ${vocabulary.tokenMap.size}")

    case Next() :: Dump() :: OptFile(file) =>
      println("== next")
      sourceFrom(vocabulary.map1ToNextPhrase.toIterator flatMap {
        case (token1, map) =>
          map.map {
            case (token2, p) =>
              (token1 :: token2 :: Nil) -> p
          }
      }).arbeiten(file)

    case Next() :: _ =>
      println(s"== tokens to next phrase size = ${vocabulary.map1ToNextPhrase.size}")

    case Inner() :: Dump() :: OptFile(file) =>
      println("== inner")
      sourceFrom(vocabulary.map1ToTheSamePhrase.toIterator flatMap {
        case (token1, map) =>
          map.map {
            case (token2, p) =>
              (token1 :: token2 :: Nil) -> p
          }
      }).arbeiten(file)

    case Inner() :: _ =>
      println(s"== tokens to the same phrase size = ${vocabulary.map1ToTheSamePhrase.size}")

    case Everything() :: _ =>
      println(
        s"""
           |Statistic:
           | - ngram1 size = ${vocabulary.nGram1.size}
           | - ngram2 size = ${vocabulary.nGram2.size}
           | - ngram3 size = ${vocabulary.nGram3.size}
           | - phrases size = ${vocabulary.statements.size}
           | - tokens size = ${vocabulary.tokenMap.size}
           | - consequent correlation size = ${vocabulary.map1ToNextPhrase.size}
           | - inner correlation size = ${vocabulary.map1ToTheSamePhrase.size}
         """.stripMargin)


    case MiniLanguage() :: words =>
      val tokens = Set(PStart.value, PEnd.value, TokenPreDef.DEOP.value, TokenPreDef.DEOW.value) ++
        /*vocabulary.auxiliary ++ */ words.flatMap(vocabulary.tokenMap(_))

      vocabulary.miniLanguageKeywords(tokens)
        .take(200)
        .map { x =>
          println(s"outcome, word = ${vocabulary.wordMap.getOrElse(x, "*UNKNOWN*")}")
          x
        }
        .runWith(Sink.seq)
        .map(_.toSet)
        .map { xs =>
          println(s"vocabulary=${xs.flatMap(vocabulary.wordMap.get(_)).mkString(" ")}")
          println(s"vocabulary size=${xs.size}")
          xs
            .flatMap(vocabulary.wordMap.get(_))
            .toList
            .sorted
            .foreach { x =>
              println(s"word=${x}")
            }

          vocabulary.statements
            .filterNot(_.exists(!xs(_)))
            .filter(_.size > 7)
            .map { x =>
              println(s"phrase = ${x.size} ${vocabulary.untokenize(x)}")
            }

        }
        .recover {
          case th: Throwable =>
            th.printStackTrace()
        }
        .await


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
      SourceExt(Source(vocabulary.statements).map { tokens =>
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
            val phrase = vocabulary.untokenize(tokens)
            println(phrase)
            history.add(phrase)
        }

    case Advice() :: Switches(switches, words) =>
      (1 to 20).foreach { n =>
        println(vocabulary.statementDenominator((1 to 20).take(n).toList))
      }

      val statement = vocabulary.tokenize(words)

      val keywords: Set[Token] = {
        val keywords: Set[Token] =
          switches.get("keywords")
            .toSet[String]
            .flatMap { x =>
              x.split(":").toSet[String].flatMap(x => vocabulary.tokenMap.getOrElse(x, Nil))
            }

        if (keywords.isEmpty) {
          if (switches.contains("keywords")) {
            statement.toSet --
              vocabulary.keywords(statement)
                .collect {
                  case (keyword, (p, _, _)) if p < 0.0 =>
                    keyword
                }
                .toSet --
              TokenPreDef.values.map(_.value).toSet
          } else {
            Set()
          }
        } else {
          keywords
        }
      }

      val originProbability = vocabulary.probability(statement) / vocabulary.statementDenominator(statement)

      val variability = switches.get("variability").map(_.toInt).getOrElse(5)

      val changeLimit = switches.get("limit").map(_.toInt).getOrElse(Math.max(statement.length / 3, 3))

      val useBest = switches.get("best").isDefined

      val uncertainty = switches.getOrElse("uncertainty", "0.0").toDouble

      val auxiliary = if (switches.contains("auxiliary"))
        vocabulary.auxiliary
      else
        Set[Token]()

      val advice = vocabulary.adviceOptimal(
        statement,
        variability = variability,
        keywords = keywords,
        auxiliary = auxiliary,
        changeLimit = changeLimit,
        uncertainty = uncertainty
      )

      advice.
        map {
          case (statement, probability) =>
            println(vocabulary.statementDenominator(statement), probability, probability / vocabulary.statementDenominator(statement) * math.pow(1.1, statement.length))

            (statement, probability, probability / vocabulary.statementDenominator(statement) * math.pow(1.1, statement.length))
        }.
        sortBy {
          _._3
        }.
        foreach {
          case (statement, probability, p) if p > uncertainty && (!useBest) || (p >= originProbability && statement.length > 4) =>
            val phrase = statement.flatMap(vocabulary.wordMap.get(_)).mkString(" ")
            println(f"$probability%16.14f $p%16.14f $phrase")

          case qq@(statement, probability, p) =>
            println(qq, p > originProbability)
        }

      keywords
        .foreach { keyword =>
          println(s"keyword = ${vocabulary.wordMap.getOrElse(keyword, "")}")
        }

      println(f"probability = $originProbability%16.14f")

      println(f"variability = $variability")

      println(f"changeLimit = $changeLimit")

      if (auxiliary.nonEmpty) {
        println(s"Vary only auxiliary words")
      }

    case Meaning() :: File1(keywords) :: File1(auxiliary) :: OptFile(weighted) =>
      def load(file: File) =
        io.Source.fromFile(file).
          getLines().
          flatMap(vocabulary.tokenMap.get(_)).
          flatten.
          toSet

      val hasSense = load(keywords)

      val hasNoSense = load(auxiliary)

      val meaningMap =
        vocabulary.meaningContextMap(hasSense, hasNoSense)

      Source.fromIterator { () =>
        vocabulary.meaningWordMap(meaningMap).
          toList.
          sortBy {
            case (token, (sense, nonsense, common)) =>
              common
          }.
          iterator
      }.
        filter {
          case (token, _) =>
            vocabulary.nGram1.get(token :: Nil).getOrElse(0) > 2 &&
              !hasSense.contains(token) && !hasNoSense.contains(token)
        }.
        mapConcat {
          case (token, (sense, nonsense, common)) =>
            vocabulary.wordMap.get(token).map { word =>
              f"$word $sense%14.12f $nonsense%14.12f $common%14.12f"
            }.toList
        }.
        arbeiten(weighted)

    case Keywords() :: words =>
      vocabulary.keywords(vocabulary.tokenizeShort(words)).sortBy(_._2._1).foreach {
        case (token, (p, p1, p2)) =>
          vocabulary.wordMap.get(token).foreach { word =>
            println(f"$word%-20s $p%5.3f ($p1%5.3f-$p2%5.3f)")
          }
      }

    case Expand() :: words =>
      println("We suggest to expande the phrase with a few words:")
      vocabulary.suggestForTheSame(vocabulary.tokenize(words)).
        flatMap {
          case (token, p) =>
            vocabulary.wordMap.get(token).map(_ -> p)
        }.
        foreach {
          case (word, p) =>
            println(s" - $word, p = $p")
        }

    case words@(_ :+ ("." | ContinuePhrase())) =>
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

    case words@(_ :: _) =>
      println("Possible continuation of the phrase: ")
      vocabulary.continueStatement(vocabulary.tokenizeShort(words)) foreach {
        case (token, probability) =>
          println(s" - ${RepresenterToken.represent(token)}, p = $probability")
      }

    case _ =>
      println(com.github.dronegator.nlp.main.Version.versionMessageExtended)
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
    consoleReader.clearScreen()
    consoleReader.getHistory.asInstanceOf[FileHistory].flush()
    consoleReader.shutdown()
  }

  override def tokenToDump(token: Token): String =
    tokenToWordString(token)
}
