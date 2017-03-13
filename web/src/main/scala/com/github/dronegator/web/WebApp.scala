package com.github.dronegator.web

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.github.dronegator.nlp.main.NLPTWebServiceMain._
import com.github.dronegator.web.WebModel._
import com.typesafe.scalalogging.LazyLogging
import shapeless.{Path => _, _}
import shapeless.ops.hlist.IsHCons
import shapeless.tag.@@
import spray.json.DefaultJsonProtocol

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

  class R1 extends Traverse[Id1 :: HNil] {
    override val path: String = "web"
  }

  class R2 extends Traverse[Id2 :: Id1 :: HNil] {
    override val path: String = "web/id1/id2"
  }

  class H1 extends Handler[H1Request, H1Response, Id1 :: HNil] {
    override def handler(request: H1Request, path: Id1 :: HNil): Future[H1Response] = ???
  }

  class H2 extends Handler[H2Request, H2Response, Id2 :: HNil] {
    override def handler(request: H2Request, path: Id2 :: HNil): Future[H2Response] = ???
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

    def description: String

    def version: String

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
      Map(
        "get" -> Map(
          "description" -> handlerHasIOP.description,
          "parameters" -> Nil,
          "responses" -> Map(
            "200" -> Map(
              "description" -> "Successful response",
              "schema" -> Map(
                "title" -> "",
                "type" -> "object",
                "properties" -> Map()
              )
            )
          )
        )
      )
    }

  implicit def schemeRouteHandler[RH <: Tuple2[R, H], R <: Traverse[_], H](implicit
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

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json._


object WebApp
  extends App
    with LazyLogging
    with WebAppTrait[MS :: M1 :: M2 :: HNil]
    with WebDescription
    with DefaultJsonProtocol {

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

  override def description: String = "Yet Another Example of Web Service"

  override def version: String = "0.0.1"

  def schemaRoute: Route =
    path("swagger-ui" / "swagger.json") { ctx =>
      implicit object AnyJsonFormat extends JsonFormat[Any] {
        def write(x: Any): JsValue = x match {
          case n: Int => JsNumber(n)
          case s: String => JsString(s)
          case b: Boolean if b == true => JsTrue
          case b: Boolean if b == false => JsFalse
          case m: Map[String, _] =>
            JsObject(m
              .map {
                case (x, y) =>
                  x -> write(y)
              }
              .toMap)
          case a: Seq[_] =>
            JsArray(a
              .map { y =>
                write(y)
              }
              .toVector)
        }

        def read(value: JsValue) = value match {
          case JsNumber(n) => n.intValue()
          case JsString(s) => s
          case JsTrue => true
          case JsFalse => false
        }
      }

      ctx.complete(Future.successful(schema.toJson))
    } ~
      pathPrefix("swagger-ui") {
        getFromResourceDirectory("swagger-ui")
      } ~
      pathPrefix("ui") {
        getFromResourceDirectory("ui")
      }

  val route = module.toList[Module[_]]
    .flatMap {
      case x: Module[HList] =>
        println(x.routes.productIterator.toList)
        x.routes.productIterator.collect {
          case (r: Traverse[_], h: Handler[_, _, _]) =>
            (r, h)
        }

    }
    .foldLeft(schemaRoute) {
      case (route: Route, (r: Traverse[_], h)) =>
        route ~ path(separateOnSlashes(r.path)) { ctx =>
          ctx.complete(s"$h")
        }

    }

  val bindingFuture = Http().bindAndHandle(route, cfg.host, cfg.port)

  logger.info(s"Server online at http://${cfg.host}:${cfg.port}/\nPress RETURN to stop...")

  Console.readLine() // for the future transformations

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown())

  logger.info(s"Server shutdown")
}
