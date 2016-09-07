package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.common.Probability
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{PEnd, PStart}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils.Match
import com.github.dronegator.nlp.vocabulary.ToolAdviceTrait._
import com.github.dronegator.nlp.vocabulary.VocabularyTools.{VocabularyTools, Advices}

/**
 * Created by cray on 9/5/16.
 */
object ToolAdviceTrait {
  type Position = Int

  object Correction {
    def unapply(correction: Correction) =
      Some(correction.position, correction.original, correction.offered)
  }

  sealed trait Correction {
    val position: Position
    val original: Fragment
    val offered: Fragment
  }

  case class Removal(position: Position, original: Fragment, offered: Fragment) extends Correction

  case class Insertion(position: Position, original: Fragment, offered: Fragment) extends Correction

  case class Exchange(position: Position, original: Fragment, offered: Fragment) extends Correction

  case class Reorder(position: Position, original: Fragment, offered: Fragment) extends Correction

  type Checker = PartialFunction[(Fragment, Fragment), List[Correction]]

  val Cut = Match[(Fragment, Fragment), (Token, Token, Token, Position)] {
    case (before :: token :: after :: _, prefix) =>
      (before, token, after, prefix.length)
  }
}

trait ToolAdviceTrait {
  this: VocabularyTools =>
  def vocabulary: Vocabulary

  private val checkers = checkIdentity :: /* checkReorder:: */ checkRemoval :: checkInsertion :: checkExchange :: Nil

  private def checkIdentity: Checker = {
    case Cut((_, token, _, position)) =>
      Exchange(position, token :: Nil, token :: Nil) :: Nil
  }

  private def checkRemoval: Checker = {
    case Cut((_, token, _, position)) =>
      Removal(position, token :: Nil, Nil) :: Nil
  }

  private def checkInsertion: Checker = {
    case Cut((_, token, after, position)) =>
      vocabulary.map2ToMiddle.get(token :: after :: Nil).
        getOrElse {
          List()
        }.
        take(4).
        map {
          case q@(_, offer) =>
            Insertion(position, token :: Nil, token :: offer :: Nil)
        }
  }

  private def checkExchange: Checker = {
    case Cut((before, token, after, position)) =>
      vocabulary.map2ToMiddle.get(before :: after :: Nil).
        getOrElse {
          List()
        }.
        take(4).
        collect {
          case (_, substitution) if substitution != token =>
            Exchange(position, token :: Nil, substitution :: Nil)
        }
  }

  private def checkReorder: Checker = {
    case Cut((before, token, _, position)) =>
      Exchange(position, before :: token :: Nil, token :: before :: Nil) :: Nil
  }


  def variety(statement: Statement) =
    Iterator.iterate((statement.drop(1), statement.take(1))) {
      case (token :: tokens, prefix) =>
        (tokens, token :: prefix)
    }.
      takeWhile(_._1.nonEmpty).
      map { cut =>
        checkers.map(_.lift(cut)).flatten.flatten
      }.
      filter(_.nonEmpty).
      toList

  def varyOnePosition(variety: List[List[ToolAdviceTrait.Correction]], statement: Statement) = {
    variety.flatMap { tokenVariety =>
      tokenVariety.map {
        case Correction(position, _, offered) =>
          val (prefix, suffix) = statement.splitAt(position + 1)
          prefix ++ offered ++ suffix.drop(1)
      }
    }
  }

  def varyOverall(variety: List[List[ToolAdviceTrait.Correction]]) = {
    def generator(variety: List[List[Correction]]): List[List[Token]] = {
      variety match {
        case head :: tail =>
          head.take(4).map {
            case Correction(_, _, offered) =>
              generator(tail) map { suffix =>
                offered ++ suffix
              }
          }.flatten
        case Nil =>
          List(List[Token]())
      }
    }

    generator(variety).
      map { statement =>

        (PStart.value :: PStart.value :: statement) :+ PEnd.value
      }
  }

  def advice(statement: Statement): Advices = {
    varyOnePosition(variety(statement), statement).
      map { statement =>
        statement -> probability(statement)
      }
  }

  def adviceOverall(statement: Statement): Advices = {
    varyOverall(variety(statement)).
      map { statement =>
        statement -> probability(statement)
      }
  }


  def adivceOptimal(statement: Statement): Advices  = {
    def advice(rStatement: Statement, after: Token): List[(Probability, Statement, Option[Token])] = {
      println(s"in: ${(after :: rStatement).map(vocabulary.wordMap(_))}")
      val out = rStatement match {
        case current :: (rStatement@(_ :: _)) =>
          val qq =              advice(rStatement, current)

          lazy val substitute = qq.flatMap {
            case (probability, statement@(before :: _), another) =>
              val token = another.getOrElse(current)

              vocabulary.map2ToMiddle
              .get(before :: after :: Nil).getOrElse(List()).filter{
                case (p, offer) => offer != current
              }
              .take(1)
              //.takeWhile(_._2 != token)
              .collect {
                case (p, offer) if current != offer =>
                  println(s"   middle ${(vocabulary.wordMap(before),vocabulary.wordMap(offer),vocabulary.wordMap(after))}")
                  (probability * p, offer :: statement, None)
              }
          }

          lazy val identical = qq collect {
            case (probability, statement@(before :: _), None) =>
              (probability, current :: statement, None)
          }

          lazy val remove = qq

          lazy val insert = qq.flatMap {
            case (probability, statement@(before :: _), another) =>
              val token = another.getOrElse(current)

              vocabulary.map2ToMiddle
                .getOrElse(before :: token ::  Nil, List())
                .take(1)
                .map {
                  case (p, offer) =>
                    println(s"   insert ${(vocabulary.wordMap(token),vocabulary.wordMap(offer),vocabulary.wordMap(before))}")
                    (probability * p,  token :: offer :: statement, None)
                }
          }

          lazy val reverse = qq.flatMap {
            case (probability, statement@(before :: _), None) =>
              (probability, after :: statement, Some(current)) :: Nil

            case _ =>
              Nil
          }

          substitute ++
          identical ++
          remove ++
            insert /*++
        reverse*/
        case current :: _ =>
          (1.0, current :: Nil, None) :: Nil
      }
      out foreach {
        case (p, statement, _) =>
          println(s" $p --> ${statement.map(vocabulary.wordMap(_))}")
      }

      out
    }

    statement.tail.reverse match {
      case token :: statement1 =>
        advice(statement1, token) map {
          case (probability, statement1, _) =>

            val s = statement.head :: (  token :: statement1).reverse

            val p = this.probability(s)

            //println(probability, p,  s)

            (s, p)
        }

      case _ =>
        Nil
    }

  }
}
