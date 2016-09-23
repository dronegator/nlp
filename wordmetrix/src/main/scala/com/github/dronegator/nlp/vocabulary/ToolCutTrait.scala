package com.github.dronegator.nlp.vocabulary

/**
 * Created by cray on 9/22/16.
 */


import com.github.dronegator.nlp.component.tokenizer.Tokenizer._
import com.github.dronegator.nlp.vocabulary.VocabularyTools.VocabularyTools

trait ToolCutTrait {
  this: VocabularyTools =>
  def vocabulary: Vocabulary

  def cut(keywords: Set[Token]): Vocabulary

}
