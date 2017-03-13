package com.github.dronegator.web

import com.github.dronegator.web.WebModel._
import shapeless._
import shapeless.ops.hlist.IsHCons
import shapeless.tag.@@

import scala.concurrent.Future

/**
  * Created by cray on 3/5/17.
  */

object WebModel {

  case class H1Request(query: String)

  case class H1Response(data: String)

  case class H2Request(query: String)

  case class H2Response(data: String)

  sealed trait Tags

  trait Id1Tag extends Tags

  trait Id2Tag extends Tags

  type Id1 = String @@ Id1Tag
  type Id2 = String @@ Id2Tag

  class R1 extends Route[Id1 :: HNil]

  class R2 extends Route[Id2 :: Id1 :: HNil]

  class H1 extends Handler[H1Request, H1Response, Id1 :: HNil] {
    override def hander(request: H1Request, path: Id1 :: HNil): Future[H1Response] = ???
  }

  class H2 extends Handler[H2Request, H2Response, Id2 :: HNil] {
    override def hander(request: H2Request, path: Id2 :: HNil): Future[H2Response] = ???
  }

  class MS extends Module[(R1, H1) :: (R2, H2) :: HNil] {
    override def routes: ::[(R1, H1), ::[(R2, H2), HNil]] =
      (new R1 -> new H1) ::
        (new R2 -> new H2) ::
        HNil

  }

  class M1 extends Module[(R1, H1) :: HNil] {
    override def routes: ::[(R1, H1), HNil] =
      (new R1 -> new H1) ::
        HNil

  }

  class M2 extends Module[(R2, H2) :: HNil] {
    override def routes: ::[(R2, H2), HNil] =
      (new R2 -> new H2) ::
        HNil
  }

  trait WebAppTrait[MS <: HList] {
    def module: MS

    def schema(implicit scheme: Scheme[WebAppTrait[MS]]) =
      scheme.gen(this)
  }

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

object Scheme extends SchemeLowPriority {

  implicit def schemeHandler[H <: Handler[_, _, _ <: HList], I, O, P <: HList](implicit handlerHasIOP: HandlerHasIOP.Aux[H, I, O, P]) =
    instance[H] { h =>
      println(handlerHasIOP.description)
      Map()
    }

  implicit def schemeRouteHandler[RH <: Tuple2[R, H], R, H](implicit
                                                            isEq: (R, H) =:= RH,
                                                            schemeH: Scheme[H]) =
    instance[RH] { x =>

      x._2
      println(s"h: ${x._2}")
      schemeH.gen(x._2)

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
      schemeM.gen(app.module)
    }
  }
}

trait Scheme[A] {
  def gen(a: A): Map[String, Any]
}

// web/runMain com.github.dronegator.web.WebApp

trait WebDescription {
  implicit val handlerH1 = HandlerHasIOP.instance[H1, H1Request, H1Response, Id1 :: HNil]("First handler")

  implicit val moduleM1 = ModuleHasRS.instance[M1, (R1, H1) :: HNil]("Module number 1")

  implicit val handlerH2 = HandlerHasIOP.instance[H2, H2Request, H2Response, Id2 :: HNil]("Second handler")

  implicit val moduleM2 = ModuleHasRS.instance[M2, (R2, H2) :: HNil]("Module number 2")

  implicit val moduleMS = ModuleHasRS.instance[MS, (R1, H1) :: (R2, H2) :: HNil]("Complex module with both handlers")
}


object WebApp
  extends App
    with WebAppTrait[MS :: M1 :: M2 :: HNil]
    with WebDescription {

  def modules: MS :: HNil = new MS :: HNil

  def modules1: M1 :: HNil = new M1 :: HNil

  def modules2: M2 :: HNil = new M2 :: HNil

  def moduless: MS :: M1 :: M2 :: HNil = new MS :: new M1 :: new M2 :: HNil

//  val scheme = Scheme[MS :: M1 :: M2 ::  HNil]
//
//  println(scheme.gen(moduless))

  //val scheme = Scheme[MS :: HNil]

  override def module = new MS :: new M1 :: new M2 :: HNil

  println(schema)

}
