package com.github.dronegator.nlp.component.tokenizer

import com.github.dronegator.nlp.component.{ComponentScan, ComponentState}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{DEOP, DEOW, Reset}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.main.TagHints
import com.github.dronegator.nlp.utils.CFG
import com.github.dronegator.nlp.vocabulary.{VocabularyHint, VocabularyHintImpl}
import com.softwaremill.tagging.@@
import enumeratum._
import enumeratum.values.IntEnumEntry

/**
 * Created by cray on 8/14/16.
 */
object Tokenizer {
  //5  6     7           8
  type Token = Int

  type Word = String

  type Statement = List[Token]

  type Fragment = List[Token]

  type Phrase = List[Word]

  type TokenMap = Map[Word, List[Token]]

  type Init = (TokenMap, Token, List[Token])

  sealed abstract class TokenPreDef extends IntEnumEntry

  case object TokenPreDef extends values.IntEnum[TokenPreDef] {
    override def values = findValues

    case object Empty extends TokenPreDef {
      val value = 0
    }

    case object PStart extends TokenPreDef {
      val value = 1
    }

    case object PEnd extends TokenPreDef {
      val value = 2
    }

    case object TEnd extends TokenPreDef {
      val value = 3
    }

    case object DEOP extends TokenPreDef {
      val value = 4
    }

    case object DEOW extends TokenPreDef {
      val value = 5
    }

    case object Reset extends TokenPreDef {
      val value = 6
    }

  }

  val MapOfPredefs = Map("." -> (DEOP.value :: DEOW.value :: Nil))


  object StopWord {
    val Punct = Set(".", ",", "'")

    def unapply(word: Word) =
    //word.find(x => !x.isLetterOrDigit).isDefined && word.find(x => !",.'".contains(x)).isDefined
      word.find(x => !x.isLetterOrDigit).isDefined && (!Punct(word))
  }

}

class Tokenizer(cfgArg: => CFG)
  extends ComponentScan[Word, (TokenMap, Token, List[Token]), (TokenMap, Token, List[Token])] {

  def cfg = cfgArg

  override def init: (TokenMap, Token, List[Token]) = (MapOfPredefs, 10, List())

  override def apply(state: (TokenMap, Token, List[Token]), word: Word): (TokenMap, Token, List[Token]) =
    (state, word) match {
      case ((map, n, _), StopWord()) =>
        (map, n, List(Reset.value))

      case ((map, n, _), w) =>
        map.get(w) match {
          case Some(tokens) =>
            (map, n, tokens)

          case None =>
            val next = n + 1: Token
            (map + (w -> List(next)), next, List(next))
        }
    }

  override val select: Select = {
    case x =>
      x
  }
}

class TokenizerWithHints(cfg: => CFG, vocabulary: VocabularyHint)
  extends Tokenizer(cfg) {
  override def init = (vocabulary.tokenMap, vocabulary.tokenMap.valuesIterator.flatten.max + 1, List.empty[Token])
}




