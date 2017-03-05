package com.github.dronegator.web

import shapeless.HList

/**
  * Created by cray on 3/5/17.
  */

object ModuleHasRS {
  def apply[RS <: HList, M <: Module[RS]](implicit moduleHasRS: ModuleHasRS.Aux[RS, M]) =
    moduleHasRS

  type Aux[RS <: HList, M <: Module[RS]] = ModuleHasRS[M] {type RS1 = RS}

  implicit def instance[RS <: HList]: Aux[RS, Module[RS]] =
    new ModuleHasRS[Module[RS]] {
      override type RS1 = RS

      override def routes(module: Module[RS]): RS =
        module.routes
    }
}

trait ModuleHasRS[M <: Module[_ <: HList]] {
  type RS1

  def routes(module: M): RS1

  /* =
     module.routes.asInstanceOf[module.RS1]*/
}


trait Module[RS <: HList] {
  type RS1 = RS

  def routes: RS
}
