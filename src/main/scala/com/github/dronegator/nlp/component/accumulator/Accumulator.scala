package com.github.dronegator.nlp.component.accumulator

import com.github.dronegator.nlp.component.Component
import com.github.dronegator.nlp.component.accumulator.Accumulator.Init
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{TokenPreDef, Token}
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/16/16.
 */

object Accumulator {

  type Init = (List[List[Token]], Option[List[Token]])
  val Init: Init = (List.empty[List[Token]], Option.empty[List[Token]])
}

class Accumulator(cfgArg: => CFG, phraseDetector: PhraseDetector) extends Component[((List[List[Token]], Option[List[Token]]), List[Token]), (List[List[Token]], Option[List[Token]])] {
  override def cfg: CFG = cfgArg

  override def apply(in: (Init, List[Token]) )  =
    in match {
    case ((buffer, _), tokens) =>
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
