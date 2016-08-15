package com.github.dronegator.nlp.component.phrase_detector

import com.github.dronegator.nlp.component.Component
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.{Token, Word}
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/15/16.
 */
object PhraseDetector {
}

class PhraseDetector(cfgArg: => CFG) extends Component[List[List[Token]], Option[(List[Token], Seq[List[Token]])]] {
  override def cfg: CFG = cfgArg

  override def apply(in: List[List[Token]]): Option[(List[Token], Seq[List[Token]])] =
    in.span{
      case tokens if tokens contains Tokenizer.TokenPreDef.DEOP.value => false
    } match {
      case (start, (dot :: rest)) =>

        val phrase = start.
          map(_.headOption).
          flatten :+ Tokenizer.TokenPreDef.DEOP.value

        Option((phrase, rest))

      case _ =>
        None
    }
}