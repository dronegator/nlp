package com.github.dronegator.nlp.component.tokenizer

import com.github.dronegator.nlp.component.Component
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{Reset, DEOW, DEOP}
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils.CFG
import enumeratum._
import enumeratum.values.{IntEnum, IntEnumEntry}

/**
 * Created by cray on 8/14/16.
 */
object Tokenizer {
  //5  6     7           8
  type Token = Int

  type Word = String

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

  val Init: Init = (MapOfPredefs, 7, List())

  object StopWord {
    val Punct = Set(".",",","'")

    def unapply(word: Word) =
      //word.find(x => !x.isLetterOrDigit).isDefined && word.find(x => !",.'".contains(x)).isDefined
      word.find(x => !x.isLetterOrDigit).isDefined && (!Punct(word))
  }

}

class Tokenizer(cfgArg: => CFG)
  extends Component[((TokenMap, Token, List[Token]), Word), (TokenMap, Token, List[Token])] {

  def cfg = cfgArg

  override def apply(x: ((TokenMap, Token, List[Token]), Word)): (TokenMap, Token, List[Token]) =
    x match {
      case ((map, n, _), qq@StopWord()) =>
        //println(s"Stop word: $qq")
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
}




