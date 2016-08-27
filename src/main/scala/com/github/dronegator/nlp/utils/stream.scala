package com.github.dronegator.nlp.utils

/**
 * Created by cray on 8/27/16.
 */
import akka.stream.{FlowShape, Graph}
import akka.stream.scaladsl.{Source, Flow}
import com.github.dronegator.nlp.component.{ComponentFold, ComponentMap}

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

  implicit class FlowStage[In, Out, Mat](val f: Flow[In, Out, Mat]) {
    def component[B](component: ComponentMap[Out, B]): Flow[In, B, Mat] = {
      f.map(component)
    }

    def componentScan[C](component: ComponentFold[Out, C]): Flow[In, C, Mat] = {
      f.scan(component.init)(component)
    }

    def componentFold[C](component: ComponentFold[Out, C]): Flow[In, C, Mat] = {
      f.fold(component.init)(component)//.asInstanceOf[M[C]]
    }
  }

  implicit class SourceStage[Out, Mat](val f: Source[Out, Mat]) {
    def component[B](component: ComponentMap[Out, B]): Source[B, Mat] = {
      f.map(component)
    }

    def componentScan[C](component: ComponentFold[Out, C]): Source[C, Mat] = {
      f.scan(component.init)(component)
    }

    def componentFold[C](component: ComponentFold[Out, C]): Source[C, Mat] = {
      f.fold(component.init)(component)//.asInstanceOf[M[C]]
    }
  }

}
