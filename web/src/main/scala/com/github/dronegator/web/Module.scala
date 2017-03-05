package com.github.dronegator.web

import shapeless.HList

/**
  * Created by cray on 3/5/17.
  */

object ModuleHasRS {
  type Aux[M <: Module[_], RS1] = ModuleHasRS[M] {type RS = RS1}

  //  implicit def instanceM1_ = ModuleHasRS.instance[M1, (R1, H1) :: HNil]

  //    implicit def instanceM1: Aux[M1, (R1, H1) :: HNil] =
  //    new ModuleHasRS[M1] {
  //      type RS = (R1, H1) :: HNil
  //
  //      override def routes(module: M1): RS =
  //        module.routes
  //    }
  //
  //  implicit def instanceM2: Aux[M2, (R2, H2) :: HNil] =
  //    new ModuleHasRS[M2] {
  //      type RS = (R2, H2) :: HNil
  //
  //      override def routes(module: M2): RS =
  //        module.routes
  //    }
  //
  //  implicit def instanceM: Aux[M, (R1, H1) :: (R2, H2) :: HNil] =
  //    new ModuleHasRS[M] {
  //      type RS = (R1, H1) :: (R2, H2) :: HNil
  //
  //      override def routes(module: M): RS =
  //        module.routes
  //    }

  def apply[M <: Module[RS1], RS1 <: HList](implicit moduleHasRS: ModuleHasRS.Aux[M, RS1]) =
    moduleHasRS

  def instance[M <: Module[RS1], RS1 <: HList](descriptionArg: String): Aux[M, RS1] =
    new ModuleHasRS[M] {
      type RS = RS1

      val description = descriptionArg
      override def routes(module: M): RS =
        module.routes
    }

}

trait ModuleHasRS[M <: Module[_]] {
  type RS

  def description: String

  def routes(module: M): RS
}


trait Module[RS <: HList] {
  def routes: RS
}
