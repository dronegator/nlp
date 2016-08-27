package com.github.dronegator.nlp.utils

/**
 * Created by cray on 8/27/16.
 */
import akka.stream.{FlowShape, Graph}
import akka.stream.scaladsl.{Source, Flow}

package object stream {

  implicit class FlowExt[In, Out, Mat](val f: Flow[In, Out, Mat]) {
    def trace(s: String) =
      f.map{ x =>
        println(s"$s: $x")
        x
      }
  }

  implicit class SourceExt[Out, Mat](val f: Source[Out, Mat]) {
    def trace(s: String) =
      f.map{ x =>
        println(s"$s: $x")
        x
      }
  }

}
