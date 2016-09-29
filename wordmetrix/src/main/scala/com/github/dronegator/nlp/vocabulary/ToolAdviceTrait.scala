package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.common.Probability
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{OtherWord, PEnd}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils._
import com.github.dronegator.nlp.vocabulary.VocabularyTools.{Advices, VocabularyTools}

/**
  * Created by cray on 9/5/16.
  */

trait ToolAdviceTrait
  extends ToolAdviceOutdatedTrait {
  this: VocabularyTools =>
  def vocabulary: Vocabulary

  def adviceOptimal(statement: Statement, keywords: Set[Token] = Set(), auxiliary: Set[Token] = Set(), changeLimit: Int = 2, uncertainty: Double = 0.0, variability: Int = 7): Advices = {
    def checkLimit(rest: (Int, Int, Probability, Statement, Option[Token])) = {
      changeLimit >= rest._1
    }

    def ifAuxilary(token: Token) =
      auxiliary.isEmpty || auxiliary.contains(token)

    def advice(rStatement: Statement): List[(Int, Int, Probability, Statement, Option[Token])] = {
      val out = rStatement match {
        case after :: current :: (rStatement@(_ :: _ :: _)) =>
          val adviceRest = advice(current :: rStatement)

          lazy val substitute = adviceRest.filter(checkLimit)
            .filterNot(keywords contains _._5.getOrElse(current))
            .filter(x => ifAuxilary(x._5.getOrElse(current)))
            .flatMap {
              case (changes, severe, probability, statement@(before :: t :: _), another) =>
                val token = another.getOrElse(current)

                vocabulary.map2ToMiddle
                  .get(before :: after :: Nil).getOrElse(List())
                  .filter {
                    case (p, offer) =>
                      offer != current
                  }
                  .take(variability)
                  .collect {
                    case (_, offer) if current != offer && ifAuxilary(offer) =>
                      val p = vocabulary
                        .pNGram3
                        .getOrElse(t :: before :: offer :: Nil, 0.0)

                      (changes + 1, severe, probability * p, offer :: statement, None)
                  }
            }

          lazy val identical = adviceRest.collect {
            case (changes, severe, probability, statement@(t1 :: t2 :: _), another) =>
              val token = another.getOrElse(current)

              val p = vocabulary
                .pNGram3
                .getOrElse(t2 :: t1 :: token :: Nil, 0.0)

              (changes, severe, p * probability, token :: statement, None)
          }

          lazy val remove =
            if (keywords.contains(current) || !ifAuxilary(current)) List()
            else
              adviceRest.filter(checkLimit)
                .map {
                  case (changes, severe, probability, statement, another) =>
                    (changes + 1, severe, probability, statement, another)
                }

          lazy val insert = adviceRest.filter(checkLimit).flatMap {
            case (changes, severe, probability, statement@(before :: t :: _), another) =>
              val token = another.getOrElse(current)

              vocabulary.map2ToMiddle
                .getOrElse(before :: token :: Nil, List())
                .filter(x => ifAuxilary(x._2))
                .take(variability)
                .map {
                  case (p, offer) =>

                    val p1 = vocabulary
                      .pNGram3
                      .getOrElse(t :: before :: offer :: Nil, 0.0)

                    val p2 = vocabulary
                      .pNGram3
                      .getOrElse(before :: offer :: token :: Nil, 0.0)

                    (changes + 1, severe, probability * p1 * p2, token :: offer :: statement, None)
                }
          }

          lazy val reverse = adviceRest
            .filter(checkLimit)
            .filterNot(keywords contains _._5.getOrElse(current))
            .flatMap {
              case (changes, severe, probability, statement@(before :: t :: _), another) =>
                val token = another.getOrElse(current)

                val p = vocabulary
                  .pNGram3
                  .getOrElse(t :: before :: after :: Nil, 0.0)

                (changes + 1, severe, probability * p, after :: statement, Some(token)) :: Nil

              case x =>
                Nil
            }

          substitute ++
            remove ++
            insert ++
            reverse ++
            identical

        case after :: statement =>
          (0, 0, 1.0, statement, None) :: Nil
      }

      out
        .toIterator
        .filter {
          _._3 > uncertainty
        }
        .filterNot {
          _._4.headOption contains OtherWord.value
        }
        .sortBy(-_._3)
        .distinctBy(_._4)
        .toList
    }

    statement.reverse match {
      case statement1 =>
        advice(statement1) map {
          case (changes, severe, probability, statement1@(t1 :: t2 :: _), _) =>

            val p1 = probability * vocabulary.pNGram3.getOrElse(t2 :: t1 :: PEnd.value :: Nil, 0.0)
            val s = (PEnd.value :: statement1).reverse

            val p = this.probability(s)

            println(probability, p, p1, s, t2 :: t1 :: PEnd.value :: Nil, vocabulary.pNGram3.getOrElse(t2 :: t1 :: PEnd.value :: Nil, 0.0))

            (s, p)
        }

      case _ =>
        Nil
    }

  }
}
