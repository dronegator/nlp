package com.github.dronegator.nlp

/**
 * Created by cray on 8/14/16.
 */
package object component {

  trait Component

  trait ComponentMap[-A, +R] extends Component with Function1[A, R]

  trait ComponentState[-A, R] extends Component with Function2[R, A, R]   {
    def init: R
  }

  trait ComponentFold[-A, R, B] extends ComponentState[A, R] {
    protected type Select = PartialFunction[R, B]

    val select: Select
  }

  trait ComponentScan[-A, R, B] extends ComponentState[A, R] {
    protected type Select = PartialFunction[R, B]

    val select: Select
  }
}
