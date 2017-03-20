package com.github.dronegator.web.example

import com.github.dronegator.web.example.model._
import com.github.dronegator.web.{HandlerHasIOP, ModuleHasRS}
import shapeless.{::, HNil}

/**
  * Created by cray on 3/20/17.
  */
trait Description {
  implicit val handlerH1 = HandlerHasIOP.instance[H1, H1Request, H1Response, Id1 :: HNil]("First handler")

  implicit val moduleM1 = ModuleHasRS.instance[M1, (R1, H1) :: HNil]("Module number 1")

  implicit val handlerH2 = HandlerHasIOP.instance[H2, H2Request, H2Response, Id2 :: HNil]("Second handler")

  implicit val moduleM2 = ModuleHasRS.instance[M2, (R2, H2) :: HNil]("Module number 2")

  implicit val moduleMS = ModuleHasRS.instance[MS, (R1, H1) :: (R2, H2) :: HNil]("Complex module with both handlers")


}
