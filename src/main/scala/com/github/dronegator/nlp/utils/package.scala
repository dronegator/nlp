package com.github.dronegator.nlp

/**
 * Created by cray on 8/18/16.
 */
package object utils {
  implicit class IteratorUnzip[A,B](val iterator: Iterator[(A,B)]) extends AnyVal {
    def unzip(): (Iterator[A], Iterator[B]) = {
      val stream = iterator.toStream
      (stream.toIterator.map(_._1), stream.toIterator.map(_._2))
    }
  }


  implicit class IteratorFork[A](val iterator: Iterator[A]) extends AnyVal {
    def fork[B]()(implicit fork: Stream[A] => B): B =
      fork(iterator.toStream)

    def fork[A]() = {
      val stream = iterator.toStream
      (stream.toIterator, stream.toIterator)
    }

    def fork3[A]() =              {
      val stream = iterator.toStream
      (stream.toIterator, stream.toIterator, stream.toIterator)
    }

    def fork4[A]() = {
      val stream = iterator.toStream
      (stream.toIterator, stream.toIterator, stream.toIterator, stream.toIterator)
    }

    def fork5[A]() = {
      val stream = iterator.toStream
      (stream.toIterator, stream.toIterator, stream.toIterator, stream.toIterator, stream.toIterator )
    }


  }
}
