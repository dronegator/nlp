package com.github.dronegator.web

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import shapeless.ops.hlist.IsHCons
import shapeless.{::, HList, HNil}

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by cray on 3/20/17.
  */


trait GenRoute[A] {
  def gen(a: A): Route
}

object GenRoute {
  def apply[A](implicit genRoute: GenRoute[A]) =
    genRoute

  def createGenRoute[A](f: A => Route) =
    new GenRoute[A] {
      override def gen(a: A): Route =
        f(a)
    }

  implicit def genRouteRoute[R <: Traverse[_], H <: Handler[_, _, _]] =
    createGenRoute[(R, H)] { rh =>
      val (r, h) = rh
      println(r.path, h)

      path(r.path) { ctx =>
        println(ctx)
        ctx.complete(h.handler(???, ???).map(_ => h.getClass().getCanonicalName))
      }

    }

  implicit def genRouteModule[M <: Module[RS], RS <: HList](implicit moduleHasRS: ModuleHasRS.Aux[M, RS],
                                                            routes: GenRoute[RS]) =
    createGenRoute[M] { module =>
      println(module.getClass.getCanonicalName)
      routes.gen(moduleHasRS.routes(module))
    }

  implicit def genRouteTraverseAndHNil[R <: Traverse[_], H <: Handler[_, _, _]](implicit genRoute: GenRoute[(R, H)]) =
    createGenRoute[(R, H) :: HNil] { rh =>
      println("======================= 1")
      genRoute.gen(rh.head)
    }

  implicit def genRouteModuleAndHNil[M <: Module[_]](implicit genRoute: GenRoute[M]) =
    createGenRoute[M :: HNil] { rh =>
      genRoute.gen(rh.head)
    }

  implicit def genRouteHConsModule[H <: Module[_], T <: HList](implicit isHCons: IsHCons.Aux[H :: T, H, T],
                                                               genRouteH: GenRoute[H],
                                                               genRouteT: GenRoute[T]) =
    createGenRoute[H :: T] { hCons =>
      println("--------------------------------------------")
      genRouteH.gen(isHCons.head(hCons)) ~ genRouteT.gen(isHCons.tail(hCons))

    }

  implicit def genRouteHConsRH[H <: Tuple2[_, _], T <: HList](implicit isHCons: IsHCons.Aux[H :: T, H, T],
                                                              genRouteH: GenRoute[H],
                                                              genRouteT: GenRoute[T]) =
    createGenRoute[H :: T] { hCons =>
      println("=======================")
      genRouteH.gen(isHCons.head(hCons)) ~ genRouteT.gen(isHCons.tail(hCons))

    }
}


trait WebServiceRoute[MS <: HList]
  extends WebAppTrait[MS] {

  def webServiceRoute(implicit genRoute: GenRoute[MS]): Route =
    genRoute.gen(module)
}
