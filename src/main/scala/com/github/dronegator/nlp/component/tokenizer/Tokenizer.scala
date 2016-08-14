package com.github.dronegator.nlp.component.tokenizer

import com.github.dronegator.nlp.component.Component
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.TokenPreDef.{DEOW, DEOP}
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

  sealed abstract class TokenPreDef extends IntEnumEntry

  case object TokenPreDef extends values.IntEnum[TokenPreDef] {
    override def values = findValues

    case object Empty extends TokenPreDef {
      val value = 0
    }

    case object DEOP extends TokenPreDef {
      val value = 1
    }

    case object DEOW extends TokenPreDef {
      val value = 2
    }

  }

  val MapOfPredefs = Map("." -> (DEOP.value :: DEOW.value :: Nil))
}

class Tokenizer(cfgArg: => CFG, tokenMap: Option[TokenMap]) extends Component[String, List[(TokenMap, List[Token])]] {
  private val rSplit = """\s+""".r

  def cfg = cfgArg

  override def apply(s: String): List[(TokenMap, List[Token])] = {
    val map = tokenMap getOrElse MapOfPredefs
    val n = map.values.flatten.max

    rSplit.split(s).scanLeft((map, n, List[Token]())) { case ((map, n, _), word) =>
      map.get(word) match {
        case Some(tokens) => (map, n, tokens)
        case None =>
          val nn = n + 1
          val tokens = (nn :: Nil)
          (map + (word -> tokens), nn, tokens)
      }
    }
  }.map { case (x, _, y) => (x, y)
  }.toList
}
