package com.github.dronegator.nlp.sample

import com.github.dronegator.nlp.sample.DBack.DBackTag
import com.github.dronegator.nlp.sample.DForth.DForthTag
import shapeless._

/**
  * Created by cray on 3/4/17.
  * utils/runMain com.github.dronegator.nlp.sample.SampleApp
  */
object SampleApp
  extends App {

  val df1 = DForth.apply[HNil]

  //implicit val df2 = DForth.apply[(Int, Int, String, Int)]

  val qd = new QD()

  val lf = qd.qd[(Int, Int, String, Int), DForthTag]((1, 2, "3", 4))

  val rf = qd.qd[(Int, Int, String, Int), DBackTag]((1, 2, "3", 4))

  println(lf, rf)

}
