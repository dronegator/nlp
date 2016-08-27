package com.github.dronegator.nlp

import com.github.dronegator.nlp.utils.CFG

/**
 * Created by cray on 8/14/16.
 */
package object component {
  trait Component {
    def cfg: CFG
  }

  trait ComponentMap[-A, +R] extends Component with Function1[A, R] {
    def cfg: CFG

    def apply(in: A): R
  }

  trait ComponentFold[-A, R] extends Component with Function2[R, A, R]{
    def cfg: CFG

    //def apply(init:R, in: A): R

    def init: R
  }

//  case class Map1() extends ComponentMap[Int, Int] {
//    val cfg = ???
//
//  }
//  val map = Map1()
//
//  List(1,2,3).map(map)
//
//  case class Fold() extends ComponentFold[String, Int] {
//    val cfg = ???
//
//    override def init: String = ""
//
//    def apply(x: String, y: Int) =
//      s"$x $y"
//  }
//
//
//  val fold = Fold()
//
//  List(1,2,3).foldLeft(fold.init)(fold)

}
