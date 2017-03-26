package com.github.dronegator.web.example

import com.github.dronegator.web.{Handler, Module, Traverse}
import shapeless.tag.@@
import shapeless.{Path => _, _}

import scala.concurrent.Future

/**
  * Created by cray on 3/20/17.
  */
object model {

  case class Q(b: Boolean,
               n: Int,
               d1: Double,
               l: Long,
               s: String)

  case class H1Request(query: String, q: Q, d: Double, q1: Q, d1: Double, n: Int, l: Long)

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
    override def handler(request: H1Request, path: Id1 :: HNil): Future[H1Response] = {
      println(request)
      Future.successful(H1Response(request.query))
    }

  }

  class H2 extends Handler[H2Request, H2Response, Id2 :: HNil] {
    override def handler(request: H2Request, path: Id2 :: HNil): Future[H2Response] = {
      println(request)
      Future.successful(H2Response(request.query))
    }
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

}
