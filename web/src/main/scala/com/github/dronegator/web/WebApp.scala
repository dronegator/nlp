package com.github.dronegator.web

import com.github.dronegator.web.WebModel.{M, M1}
import shapeless._
import shapeless.ops.hlist.IsHCons
import shapeless.ops.tuple.IsComposite
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

  class M extends Module[(R1, H1) :: (R2, H2) :: HNil] {
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

}

trait SchemeLowPriority {
  def apply[A](implicit scheme: Scheme[A]) =
    scheme


  def instance[A](f: A => Map[String, Any]) =
    new Scheme[A] {
      override def gen(a: A): Map[String, Any] =
        f(a)
    }

  implicit def schemeRoutes[RS <: HList, R, H, RH, T <: HList](implicit isHCons: IsHCons.Aux[RS, RH, T],
                                                               isEqRH: IsComposite.Aux[RH, R, H],
                                                               schemeT: Lazy[Scheme[T]]) =
    instance[RS] { routes =>

      val rh = isHCons.head(routes)
      val h = isEqRH.head(rh)
      val r = isEqRH.tail(rh)

      println(s"Route: $r -> $h")

      schemeT.value.gen(isHCons.tail(routes))
    }


  //  implicit def schemeModule[RS <: HList, M <: Module[RS]](implicit schemeRS: Scheme[RS]) =
  //    instance[M] { m =>
  //
  //      schemeRS.gen(m.routes)
  //    }

}

object Scheme extends SchemeLowPriority {

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
      println(s"Module: $m")
      schemeRS.value.gen(module.value.routes(m)) ++
        schemeT.gen(isHCons.value.tail(modules))

    }
}

trait Scheme[A] {
  def gen(a: A): Map[String, Any]
}

// web/runMain com.github.dronegator.web.WebApp
object WebApp
  extends App {

  def modules: M :: HNil = new M :: HNil

  def modules1: M1 :: HNil = new M1 :: HNil

  def moduless: M1 :: M :: HNil = new M1 :: new M :: HNil

  val scheme = Scheme[M1 :: M :: HNil]
  println(scheme.gen(moduless))
}
