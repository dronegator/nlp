package com.github.dronegator.web

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.Materializer
import shapeless.ops.hlist.IsHCons
import shapeless.{::, HList, HNil}

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by cray on 3/25/17.
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

  implicit def genRouteRoute[R <: Traverse[_], H <: Handler[I1, O1, P1], I1, O1, P1 <: HList](implicit handlerHasIOP: HandlerHasIOP.Aux[H, I1, O1, P1],
                                                                                              parseRequest: RequestExtractor[I1],
                                                                                              responseSerialize: ResponseSerializer[O1],
                                                                                              mat: Materializer) =
    createGenRoute[(R, H)] { rh =>
      val (r, h) = rh
      path(r.path) { ctx =>
        parseRequest.extract(ctx)
          .flatMap { i1 =>
            h.handler(i1, null.asInstanceOf[P1])
          }
          .flatMap { o1 =>
            responseSerialize.serialize(o1)
          }
          .flatMap { json =>
            ctx.complete(json)
          }
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
      genRouteH.gen(isHCons.head(hCons)) ~ genRouteT.gen(isHCons.tail(hCons))

    }

  implicit def genRouteHConsRH[H <: Tuple2[_, _], T <: HList](implicit isHCons: IsHCons.Aux[H :: T, H, T],
                                                              genRouteH: GenRoute[H],
                                                              genRouteT: GenRoute[T]) =
    createGenRoute[H :: T] { hCons =>
      genRouteH.gen(isHCons.head(hCons)) ~ genRouteT.gen(isHCons.tail(hCons))

    }
}
