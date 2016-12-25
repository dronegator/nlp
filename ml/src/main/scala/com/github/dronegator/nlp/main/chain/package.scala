package com.github.dronegator.nlp.main

import breeze.linalg.SparseVector
import com.github.dronegator.nlp.component.tokenizer.Tokenizer.Token

/**
  * Created by cray on 12/15/16.
  *
  * NN to select keywords in a phrase from the closest context.
  */
package object chain {
  type I = (Token, Token)
  type O = SparseVector[Double]
}
