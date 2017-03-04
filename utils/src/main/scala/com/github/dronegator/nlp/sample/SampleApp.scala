package com.github.dronegator.nlp.sample

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

  println(qd.qd[(Int, Int, String, Int), DForth[(Int, Int, String, Int)]]((1, 2, "3", 4)))

  println(qd.qd[(Int, Int, String, Int), DBack[(Int, Int, String, Int)]]((1, 2, "3", 4)))
}
