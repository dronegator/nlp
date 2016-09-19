package com.github.dronegator.nlp

/**
 * Created by cray on 9/15/16.
 */
package object trace {
  implicit class Trace[A](data: =>A) {
    def timeValue(f: (Long, A) => Unit) = {
      val t1 = System.currentTimeMillis()
      val a = data
      val t2 = System.currentTimeMillis()
      f(t2-t1, a)
      a
    }
    def time(f: Long => Unit) = {
      val t1 = System.currentTimeMillis()
      val a = data
      val t2 = System.currentTimeMillis()
      f(t2-t1)
      a
    }
  }
}