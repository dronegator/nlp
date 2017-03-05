package com.github.dronegator.web

import com.github.dronegator.web.WebModel._
import shapeless.{::, HList, HNil}

/**
  * Created by cray on 3/5/17.
  */

object ModuleHasRS {
  type Aux[M <: Module[_], RS1] = ModuleHasRS[M] {type RS = RS1}

  implicit def instanceM1: Aux[M1, (R1, H1) :: HNil] =
    new ModuleHasRS[M1] {
      type RS = (R1, H1) :: HNil

      override def routes(module: M1): RS =
        module.routes
    }

  implicit def instanceM2: Aux[M2, (R2, H2) :: HNil] =
    new ModuleHasRS[M2] {
      type RS = (R2, H2) :: HNil

      override def routes(module: M2): RS =
        module.routes
    }

  implicit def instanceM: Aux[M, (R1, H1) :: (R2, H2) :: HNil] =
    new ModuleHasRS[M] {
      type RS = (R1, H1) :: (R2, H2) :: HNil

      override def routes(module: M): RS =
        module.routes
    }
}

trait ModuleHasRS[M <: Module[_]] {
  type RS

  def routes(module: M): RS
}


trait Module[RS <: HList] {
  type RS1 = RS

  def routes: RS
}
