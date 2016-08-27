package com.github.dronegator.nlp

import com.github.dronegator.nlp.component.{ComponentFold, ComponentMap}

import scala.math.Ordering

/**
 * Created by cray on 8/18/16.
 */
package object utils {

  implicit class IteratorUnzip[A, B](val iterator: Iterator[(A, B)]) extends AnyVal {
    def unzip(): (Iterator[A], Iterator[B]) = {
      val stream = iterator.toStream
      (stream.toIterator.map(_._1), stream.toIterator.map(_._2))
    }
  }

  implicit class IteratorFork[A](val iterator: Iterator[A]) extends AnyVal {
    private def stream = iterator.toStream

    def fork[B]()(implicit fork: Stream[A] => B): B =
      fork(stream)

    def fork[A]() =
      (stream.toIterator, stream.toIterator)

    def fork3[A]() =
      (stream.toIterator, stream.toIterator, stream.toIterator)

    def fork4[A]() =
      (stream.toIterator, stream.toIterator, stream.toIterator, stream.toIterator)

    def fork5[A]() =
      (stream.toIterator, stream.toIterator, stream.toIterator, stream.toIterator, stream.toIterator)

    def sortBy[B](f: A => B)(implicit ord: Ordering[B]) =
      stream.sortBy[B](f)(ord)

    def sorted[B >: A](implicit ord: Ordering[B]) =
      stream.sorted[B](ord)
  }

  implicit class IteratorLog[A, M[A] <: TraversableOnce[A]](a: M[A]) {
    def trace(s: String=""): M[A] = {
      a.map/*[A, Seq[A]]*/{x =>
        println(s"$s $x")
        x
      }.asInstanceOf[M[A]]
    }
  }

  implicit class IteratorStage[A, M[A] <: Iterator[A]](a: M[A]) {
    def component[B](component: ComponentMap[A, B]): M[B] = {
      a.map/*[A, Seq[A]]*/(component).asInstanceOf[M[B]]
    }

    def componentScan[C](component: ComponentFold[A, C]): M[C] = {
      a.scanLeft(component.init)(component).asInstanceOf[M[C]]
    }

    def componentFold[C](component: ComponentFold[A, C]): C = {
      a.foldLeft(component.init)(component)//.asInstanceOf[M[C]]
    }
  }

}

