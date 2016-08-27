package com.github.dronegator.nlp.component.phrase_detector

import com.github.dronegator.nlp.component.ComponentMap
import com.github.dronegator.nlp.component.tokenizer.Tokenizer
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{PEnd, PStart}
import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/15/16.
 */
object PhraseDetector {
}

class PhraseDetector(cfgArg: => CFG) extends ComponentMap[List[List[Token]], Option[(List[Token], Seq[List[Token]])]] {
  override def cfg: CFG = cfgArg

  override def apply(in: List[List[Token]]): Option[(List[Token], Seq[List[Token]])] =
    in.span {
      case tokens if tokens contains Tokenizer.TokenPreDef.DEOP.value => false
      case tokens if tokens contains Tokenizer.TokenPreDef.TEnd.value => false
      case _ => true
    } match {
      case (start, (tokens :: rest)) if tokens contains Tokenizer.TokenPreDef.TEnd.value =>

        val phrase = start.flatMap(_.headOption)

        Option((PStart.value +: PStart.value +: phrase :+ PEnd.value, Nil))

      case (start, (dot :: rest)) =>

        val phrase = start.
          flatMap(_.headOption) :+
          Tokenizer.TokenPreDef.DEOP.value


        Option((PStart.value +: PStart.value +: phrase :+ PEnd.value, rest))

      case _ =>
        None
    }
}