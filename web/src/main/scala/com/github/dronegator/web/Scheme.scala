package com.github.dronegator.web

import shapeless.ops.hlist.IsHCons
import shapeless.{::, HList, HNil, Lazy}

/**
  * Created by cray on 3/20/17.
  */
trait Scheme[A] {
  def gen(a: A): Map[String, Any]
}

trait SchemeLowPriority {
  def apply[A](implicit scheme: Scheme[A]) =
    scheme


  def instance[A](f: A => Map[String, Any]) =
    new Scheme[A] {
      override def gen(a: A): Map[String, Any] =
        f(a)
    }
}

trait SchemeTrait extends SchemeLowPriority {

  implicit def schemeHandler[H <: Handler[_, _, _ <: HList], I, O, P <: HList](implicit handlerHasIOP: HandlerHasIOP.Aux[H, I, O, P],
                                                                               caseClassSchemeI: CaseClassScheme[I],
                                                                               caseClassSchemeO: CaseClassScheme[O]) =
    instance[H] { h =>
      println(handlerHasIOP.description)
      Map(
        "post" -> Map(
          "description" -> handlerHasIOP.description,
          "parameters" -> (
            Map(
              "name" -> "request",
              "in" -> "body",
              "description" -> s"request for ${handlerHasIOP.description}",
              "required" -> true,
              "schema" -> caseClassSchemeI.scheme
            ) :: Nil),
          "produces" -> ("application/json" :: Nil),
          "responses" -> Map(
            "200" -> Map(
              "description" -> "Successful response",
              "schema" -> caseClassSchemeO.scheme
            )
          )
        )
      )
    }

  implicit def schemeTraverseHandler[RH <: Tuple2[R, H], R <: Traverse[_], H](implicit
                                                                              isEq: (R, H) =:= RH,
                                                                              schemeH: Scheme[H]) =
    instance[RH] { x =>
      println(s"h: ${x._2}")

      Map(
        s"/${x._1.path}" -> schemeH.gen(x._2)
      )
    }

  implicit def schemeHCons[H, T <: HList](implicit isHCons: IsHCons.Aux[H :: T, H, T],
                                          schemeH: Scheme[H],
                                          schemeT: Scheme[T]) =
    instance[H :: T] { ht =>

      println(s"ht $ht")
      schemeH.gen(isHCons.head(ht)) ++
        schemeT.gen(isHCons.tail(ht))
    }

  implicit def schemeHNil: Scheme[HNil] =
    instance[HNil] { _ =>
      Map()
    }

  implicit def schemeModules[MS <: HList, RS <: HList, M <: Module[_ <: HList], T <: HList](implicit isHCons: Lazy[IsHCons.Aux[MS, M, T]],
                                                                                            module: Lazy[ModuleHasRS.Aux[M, RS]],
                                                                                            schemeRS: Lazy[Scheme[RS]],
                                                                                            schemeT: Scheme[T]
                                                                                           ) =
    instance[MS] { modules =>
      val m = isHCons.value.head(modules)
      println(s"Module: $m, ${module.value.description}")
      schemeRS.value.gen(module.value.routes(m)) ++
        schemeT.gen(isHCons.value.tail(modules))

    }

  implicit def schemeApp[MS <: HList](implicit schemeM: Scheme[MS]) = {
    instance[WebAppTrait[MS]] { app =>
      println(s"WebApp: $app")

      Map(
        "swagger" -> "2.0",
        "info" -> Map(
          "version" -> app.version,
          "title" -> app.description
        ),
        "paths" -> schemeM.gen(app.module)
      )
    }
  }
}

object Scheme extends SchemeTrait