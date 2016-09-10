package com.github.dronegator.nlp.utils

import java.io.File

/**
 * Created by cray on 8/25/16.
 */
object Match{
  def apply[A,B](pf: PartialFunction[A, B]) = new Match[A, B](pf)

  val LastOption = Match[List[String], Option[String]]{case x => x.lastOption }

  val OptFile = Match[List[String], Option[File]]{
    case Nil => None
    case file :: Nil => Some(new File(file))
  }

  val  File1 = Match[String, File]{
    case file => new File(file)
  }
  
  val RSwitch = "--(\\S*)=(\\S*)".r
  
  val RSwitchUse = "--use-(\\S*)".r

  object Switches {
    def unapply(seq: Seq[String]) = Some{
      seq
        .foldLeft((Map[String, String](), List[String]())){
          case ((switches, args), RSwitch(key, value)) =>
            (switches + (key -> value), args)

          case ((switches, args), RSwitchUse(key)) =>
            (switches + (key -> ""), args)

          case ((switches, args), arg) =>
            (switches, args :+ arg)
        }
    }
  }
  
}

class Match[A,B](pf: PartialFunction[A, B]) {
  def unapply(a: A) = pf.lift(a)
}


