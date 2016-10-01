package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.component.tokenizer.Tokenizer._

/**
 * Created by cray on 9/25/16.
 */
package object ToolMiniLanguage {

  sealed trait QueueMessage

  case class QueueMessageAdd(token: Token, p: Int) extends QueueMessage

  implicit class VocabularyToolsAkka(val vocabulary: Vocabulary) extends ToolMiniLanguageTrait

  case object QueueMessageGet extends QueueMessage


}
