package com.github.dronegator.nlp.component.phrase_correlation_consequent

/**
 * Created by cray on 8/19/16.
 */

import com.github.dronegator.nlp.component.{ComponentFold, ComponentState}
import com.github.dronegator.nlp.component.phrase_correlation_consequent.PhraseCorrelationConsequentWithHints.Init
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.main.TagHints
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.vocabulary.{VocabularyHint }
import com.github.dronegator.nlp.vocabulary.VocabularyTools.VocabularyHintTools
import com.softwaremill.tagging.@@


object PhraseCorrelationConsequentWithHints {
  type Init = (List[Token], Map[List[Token], Int])
}


class PhraseCorrelationConsequentWithHints(cfgArg: CFG, vocabularyHint: VocabularyHint) extends ComponentFold[List[Token], Init, Map[List[Token], Int]] {

  private val toWord =
    vocabularyHint.tokenMap.flatMap{
      case (word, tokens) =>
        tokens.map{
          case (token) =>
            token -> word
        }
    }.withDefaultValue("unknown")

  override def cfg: CFG = cfgArg

  override def init = (List[Token](), Map[List[Token], Int]())

  override def apply(state: Init, statement: List[Token]): (List[Token], Map[List[Token], Token]) =
    state match {
      case (prev, map) =>
        //println(prev.map(toWord).mkString(" "))
        val prevSens = vocabularyHint.keywords(prev).collect{
          case (token, (p, _, _)) if p > 0 =>
          //  println(toWord(token), p)
            token
        }

        val statementSens = vocabularyHint.keywords(statement).collect{
          case (token, (p, _, _)) if p > 0 =>
            token
        }

        val newmap = (for {
          p <- prevSens
          n <- statementSens
        } yield (p :: n :: Nil)).
          foldLeft(map) {
            case (map, pair) =>
              map + (pair -> (map.getOrElse(pair, 0) + 1))
          }
        (statement, newmap)
    }

  override val select: Select = {
    case (_, map) =>
      map
  }
}
/**
 * Created by cray on 9/3/16.
 */
