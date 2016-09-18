package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.common.Probability
import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.utils._
import com.github.dronegator.nlp.vocabulary
import scala.util.Try

/**
 * Created by cray on 9/3/16.
 */
object VocabularyHintImpl {
  implicit def apply(vocabulary: VocabularyRaw): VocabularyHint =
    VocabularyHintImpl(
      vocabulary.tokenMap,
      vocabulary.meaningMap
    )

}

trait VocabularyHintWords {
  this: vocabulary.VocabularyHint =>

  private def loadHints(name: String) =
    Try {
      io.Source.
        fromInputStream(classOf[VocabularyImpl].getResourceAsStream(name)).
        getLines().
        map(_.trim).
        filter(_.nonEmpty).
        //trace(s"$name word: ").
        flatMap(tokenMap.get(_)).
        flatten.
        toSet
    } getOrElse {
      println(s"Resource name=$name is unavailable")
      Set[Token]()
    }

  lazy val sense: Set[Token] = loadHints("/hint/english/sense.txt")
  lazy val auxiliary: Set[Token] = loadHints("/hint/english/auxiliary.txt")
}

case class VocabularyHintImpl(tokenMap: Map[Word, List[Token]],
                              meaningMap: Map[(Token, Token), (Probability, Probability)])
  extends VocabularyHint
  with VocabularyHintWords
