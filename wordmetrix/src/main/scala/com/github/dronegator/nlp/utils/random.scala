package com.github.dronegator.nlp.utils

import scala.util.Random

/**
 * Created by cray on 8/23/16.
 */
object RandomUtils {
  def randomWeightedChoice[A](choices: Seq[(A, Double)]): Option[A] =
    randomWeightedChoice(
      choices,
      choices.
        map(_._2).
        reduceOption(_ + _).
        getOrElse(0))

  def randomWeightedChoice[A](choices: Seq[(A, Double)], sum: Double): Option[A] = {

    val random = Random.nextDouble() * sum
    //println(sum, random, choices)
    //println(s"---- $sum")
    choices.toIterator.
      scanLeft((0.0, Option.empty[A])) {
        case ((d, _), (value, p)) =>
          //println(s" $d ??? $random / $p")
          val nd = d + p
          if (nd < random)
            (nd, None)
          else
            (nd, Some(value))
      }.
      collectFirst {
        case (_, Some(value)) => value
      }
  }

  implicit class RandomUtils[A](val v: Seq[(Double, A)]) extends AnyVal {
    def choiceOption(sum: Double): Option[A] = randomWeightedChoice(v.map { case (x, y) => (y, x) }.reverse, sum)

    def choiceOption(): Option[A] = randomWeightedChoice(v.map { case (x, y) => (y, x) }.reverse)
  }

}