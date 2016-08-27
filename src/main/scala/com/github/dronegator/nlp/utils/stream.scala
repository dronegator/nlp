package com.github.dronegator.nlp.utils

/**
 * Created by cray on 8/27/16.
 */

import akka.stream.scaladsl.{Flow, Source}
import com.github.dronegator.nlp.component.{ComponentFold, ComponentMap}

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

    def componentScan[C](component: ComponentFold[Out, C]): Flow[In, C, Mat] = {
      f.scan(component.init)(component)
    }

    def componentFold[C](component: ComponentFold[Out, C]): Flow[In, C, Mat] = {
      f.fold(component.init)(component) //.asInstanceOf[M[C]]
    }

    def progress(chunk: Int = 1024 * 10) = f.via(progressFlow(chunk))
  }

  implicit class SourceStage[Out, Mat](val f: Source[Out, Mat]) {
    def component[B](component: ComponentMap[Out, B]): Source[B, Mat] = {
      f.map(component)
    }

    def componentScan[C](component: ComponentFold[Out, C]): Source[C, Mat] = {
      f.scan(component.init)(component)
    }

    def componentFold[C](component: ComponentFold[Out, C]): Source[C, Mat] = {
      f.fold(component.init)(component) //.asInstanceOf[M[C]]
    }

    def progress(chunk: Int = 1024 * 10) = f.via(progressFlow(chunk))
  }

}
