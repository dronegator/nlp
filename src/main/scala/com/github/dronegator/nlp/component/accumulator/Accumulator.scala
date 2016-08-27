package com.github.dronegator.nlp.component.accumulator

import com.github.dronegator.nlp.component.ComponentFold
import com.github.dronegator.nlp.component.accumulator.Accumulator.Init
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, TokenPreDef}
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/16/16.
 */

object Accumulator {
  type Init = (List[List[Token]], Option[List[Token]])
}

class Accumulator(cfgArg: => CFG, phraseDetector: PhraseDetector)
  extends ComponentFold[List[Token], Init] {
  override def cfg: CFG = cfgArg

  override def init: (List[List[Token]], Option[List[Token]]) = (List.empty[List[Token]], Option.empty[List[Token]])

  override def apply(state: (List[List[Token]], Option[List[Token]]), tokens: List[Token]): (List[List[Token]], Option[List[Token]]) =
    state match {
      case (buffer, _) =>
        val tokenizedText = buffer :+ tokens

        phraseDetector(tokenizedText) match {
          case Some((phrase, rest)) if phrase.contains(TokenPreDef.Reset.value) =>
            (rest.toList, Some(Nil))

          case Some((phrase, rest)) =>
            (rest.toList, Some(phrase))

          case None =>
            (tokenizedText, None)
        }
    }

}
