package com.github.dronegator.nlp.sample

import com.github.dronegator.nlp.sample.DBackImpl.DBack
import com.github.dronegator.nlp.sample.DForthImpl.DForth

/**
  * Created by cray on 3/4/17.
  * utils/runMain com.github.dronegator.nlp.sample.QServiceApp
  */
object QServiceApp
  extends App {

  val qd = new QService()

  // Wrong way
  val l1f = qd.dForth((1, 2, "3", 4))

  val l1b = qd.dBack((1, 2, "3", 4))

  // Extendable way
  val l2f = qd.tag[DForth].doD((1, 2, "3", 4))

  val l2b = qd.tag[DBack].doD((1, 2, "3", 4))

  println(l1f, l1b)

  println(l2f, l2b)
}
