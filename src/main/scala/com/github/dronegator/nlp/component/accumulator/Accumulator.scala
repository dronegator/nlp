package com.github.dronegator.nlp.component.accumulator

import com.github.dronegator.nlp.component.Component
import com.github.dronegator.nlp.component.phrase_detector.PhraseDetector
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/16/16.
 */

object Accumulator {

}

class Accumulator(cfgArg: => CFG, phraseDetector: PhraseDetector) extends Component[((List[List[Token]], Option[List[Token]]), List[Token]), (List[List[Tokenizer.Token]], Option[List[Tokenizer.Token]])] {
  override def cfg: CFG = cfgArg

  override def apply(in: ((List[List[Token]], Option[List[Token]]), List[Token]) )  =
    in match {
    case ((buffer, _), tokens) =>
      val tokenizedText = buffer :+ tokens

      phraseDetector(tokenizedText) match {
        case Some((phrase, rest)) =>
          (rest.toList, Some(phrase))

        case None =>
          (tokenizedText, None)
      }
  }

}
