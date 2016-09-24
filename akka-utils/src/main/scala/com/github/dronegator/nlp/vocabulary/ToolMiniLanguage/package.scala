package com.github.dronegator.nlp.vocabulary

import com.github.dronegator.nlp.component.tokenizer.Tokenizer._

/**
 * Created by cray on 9/25/16.
 */
package object ToolMiniLanguage {

  sealed trait QueueMessage

  case class QueueMessageAdd(token: Token) extends QueueMessage

  case object QueueMessageGet extends QueueMessage

}
