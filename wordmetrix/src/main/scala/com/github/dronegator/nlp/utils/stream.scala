package com.github.dronegator.nlp.utils

/**
 * Created by cray on 8/27/16.
 */

import akka.stream.scaladsl.{Flow, Source}
import com.github.dronegator.nlp.component.{ComponentFold, ComponentScan, ComponentState, ComponentMap}

package object stream {

  implicit class FlowExt[In, Out, Mat](val f: Flow[In, Out, Mat]) {
    def trace(s: String) =
      f.map { x =>
        println(s"$s: $x")
        x
      }
  }

  implicit class SourceExt[Out, Mat](val f: Source[Out, Mat]) {
    def trace(s: String) =
      f.map { x =>
        println(s"$s: $x")
        x
      }
  }

  def progressFlow[A](chunk: Int = 1024 * 10) =
    Flow[A].
      scan((0, Option.empty[A])) {
        case ((n, _), item) =>
          if (n % chunk == 0) {
            println(f"$n%20d items passed through ${Runtime.getRuntime().freeMemory()} ${Runtime.getRuntime().maxMemory()}")
          }

          val m = item match {
            case x: String =>
              n + x.length
            case x: Seq[_] =>
              n + x.length
            case _ => n + 1
          }

          (m, Some(item))
      }.
      collect {
        case (_, Some(item)) =>
          item
      }


  implicit class FlowStage[In, Out, Mat](val f: Flow[In, Out, Mat]) {
    def component[B](component: ComponentMap[Out, B]): Flow[In, B, Mat] = {
      f.map(component)
    }

    def component[B, C](component: ComponentScan[Out, C, B]): Flow[In, B, Mat] = {
      f.scan(component.init)(component).collect(component.select)
    }

    def component[B, C](component: ComponentFold[Out, C, B]): Flow[In, B, Mat] = {
      f.fold(component.init)(component).collect(component.select)
    }

    def componentScan[C](component: ComponentState[Out, C]): Flow[In, C, Mat] = {
      f.scan(component.init)(component)
    }

    def componentFold[C](component: ComponentState[Out, C]): Flow[In, C, Mat] = {
      f.fold(component.init)(component) //.asInstanceOf[M[C]]
    }

    def progress(chunk: Int = 1024 * 10) = f.via(progressFlow(chunk))
  }

  implicit class SourceStage[Out, Mat](val f: Source[Out, Mat]) {
    def component[B](component: ComponentMap[Out, B]): Source[B, Mat] = {
      f.map(component)
    }

    def component[B, C](component: ComponentScan[Out, C, B]): Source[B, Mat] = {
      f.scan(component.init)(component).collect(component.select)
    }

    def component[B, C](component: ComponentFold[Out, C, B]): Source[B, Mat] = {
      f.fold(component.init)(component).collect(component.select)
    }

    def componentScan[C](component: ComponentState[Out, C]): Source[C, Mat] = {
      f.scan(component.init)(component)
    }

    def componentFold[C](component: ComponentState[Out, C]): Source[C, Mat] = {
      f.fold(component.init)(component) //.asInstanceOf[M[C]]
    }

    def progress(chunk: Int = 1024 * 10) = f.via(progressFlow(chunk))
  }

}
