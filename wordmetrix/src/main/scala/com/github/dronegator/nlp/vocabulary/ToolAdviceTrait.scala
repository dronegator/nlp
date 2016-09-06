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

  private val checkers = /* checkIdentity :: checkReorder:: */ checkRemoval :: checkInsertion :: checkExchange :: Nil

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
      //println(variety.length)
      variety match {
        case head :: tail =>
          head.map {
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
    //varyOverall(variety(statement)).
      map { statement =>
        //println(statement)
        statement -> probability(statement)
      }
  }
}
