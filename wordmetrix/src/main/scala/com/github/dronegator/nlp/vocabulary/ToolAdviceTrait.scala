package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.common.Probability
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{PEnd, PStart}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils.Match
import com.github.dronegator.nlp.vocabulary.VocabularyTools.{VocabularyTools, Advices}
import com.github.dronegator.nlp.utils._

/**
 * Created by cray on 9/5/16.
 */

trait ToolAdviceTrait
  extends ToolAdviceOutdatedTrait {
  this: VocabularyTools =>
  def vocabulary: Vocabulary

  def adviceOptimal(statement: Statement, changeLimit: Int = 3): Advices = {
    def checkLimit(rest: (Int, Int, Probability, Statement, Option[Token])) = {
      changeLimit >= rest._1 && rest._3 > 0
    }

    def headProbability(statement: Statement, token: Token) =
      statement match {
        case t1 :: t2 :: _ =>
          //println(s"   prob ${(vocabulary.wordMap(t2),vocabulary.wordMap(t1),vocabulary.wordMap(token))}")
          vocabulary
            .pNGram3
            .getOrElse(t2 :: t1 :: token :: Nil, 0.0)

        case _ =>
          1.0
      }

    def advice(rStatement: Statement): List[(Int, Int, Probability, Statement, Option[Token])] = {
      //println(s"in: ${(rStatement).map(vocabulary.wordMap(_))}")
      val out = rStatement match {
        case after :: current :: (rStatement@(_ :: _)) =>
          val adviceRest = advice(current :: rStatement)

          lazy val substitute = adviceRest.filter(checkLimit).flatMap {
            case (changes, severe, probability, statement@(before :: _), another) =>
              val token = another.getOrElse(current)

              vocabulary.map2ToMiddle
                .get(before :: after :: Nil).getOrElse(List()).filter{
                  case (p, offer) => offer != current
                }
                .take(5)
                //.takeWhile(_._2 != token)
                .collect {
                  case (p, offer) if current != offer =>
                    //println(s"   middle ${(vocabulary.wordMap(before),vocabulary.wordMap(offer),vocabulary.wordMap(after))}")
                    (changes + 1, severe, probability * headProbability(statement, offer), offer :: statement, None)
                }
          }

          lazy val identical = adviceRest.collect {
            case (changes, severe, probability, statement@(before :: _), another) =>
              val token = another.getOrElse(current)
              (changes, severe, probability * headProbability(statement, token), token :: statement, None)
          }

          lazy val remove = adviceRest.filter(checkLimit)
            .map {
              case (changes, severe, probability, statement@(before :: _), another) =>
                (changes + 1, severe, probability, statement, another)
            }

          lazy val insert = adviceRest.filter(checkLimit).flatMap {
            case (changes, severe, probability, statement@(before :: _), another) =>
              val token = another.getOrElse(current)

              vocabulary.map2ToMiddle
                .getOrElse(before :: token :: Nil, List())
                .take(5)
                .map {
                  case (p, offer) =>
                    //println(s"   insert ${(vocabulary.wordMap(token), vocabulary.wordMap(offer), vocabulary.wordMap(before))}")
                    (changes + 1, severe, probability * headProbability(offer :: statement, token), token :: offer :: statement, None)
                }
          }

          lazy val reverse = adviceRest.filter(checkLimit).flatMap {
            case (changes, severe, probability, statement@(before :: _), another) =>
              val token = another.getOrElse(current)
              (changes + 1, severe, probability * headProbability(statement, after), after :: statement, Some(token)) :: Nil

            case _ =>
              Nil
          }

          substitute ++
            remove ++
            insert ++
            identical ++
            reverse

        case after :: current :: _ =>
          (0, 0, 1.0, current :: Nil, None) :: Nil
      }

      //      out foreach {
      //        case (changes, severe, p, statement, _) =>
      //          println(s" $changes $p --> ${statement.map(vocabulary.wordMap(_))}")
      //      }

      out.toIterator.distinctBy(_._4).toList
    }

    statement.tail.reverse match {
      case statement1 =>
        advice(statement1) map {
          case (changes, severe, probability, statement1, _) =>

            val s = statement.head :: (statement1).reverse

            val p = this.probability(s)

            //      println(probability, p,  s)

            (s, p)
        }

      case _ =>
        Nil
    }

  }
}
