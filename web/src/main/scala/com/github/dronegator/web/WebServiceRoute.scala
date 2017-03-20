package com.github.dronegator.web

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import shapeless.HList
import shapeless.ops.hlist.ToTraversable

/**
  * Created by cray on 3/20/17.
  */
trait WebServiceRoute[MS <: HList]
  extends WebAppTrait[MS] {

  def webServiceRoute(implicit toTraversableAux: ToTraversable.Aux[MS, List, Module[_]]): Route =
    module.toList[Module[_]]
      .flatMap {
        case x: Module[HList] =>
          println(x.routes.productIterator.toList)
          x.routes.productIterator.collect {
            case (r: Traverse[_], h: Handler[_, _, _]) =>
              (r, h)
          }
      }
      .map {
        case (r: Traverse[_], h: Handler[_, _, _]) =>
          path(separateOnSlashes(r.path)) { ctx =>
            ctx.complete(s"$h")
          }
      }
      .reduceLeft[Route] {
      case (route: Route, r: Route) =>
        route ~ r
    }
}
