package com.github.dronegator.nlp.utils

/**
 * Created by cray on 8/25/16.
 */
object Match{
  def apply[A,B](pf: PartialFunction[A, B]) = new Match[A, B](pf)
  val LastOption = Match[List[String], Option[String]]{case x => x.lastOption }
}

class Match[A,B](pf: PartialFunction[A, B]) {
  def unapply(a: A) = pf.lift(a)
}


