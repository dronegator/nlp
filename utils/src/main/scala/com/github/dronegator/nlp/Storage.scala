package com.github.dronegator.nlp

import shapeless._
import shapeless.tag._

/**
  * Created by cray on 12/19/16.
  */

object Storage {
  def apply[A, B](implicit sql: Storage[String @@ B, A, B]) =
    sql

  def instance[AI, A, B](createArg: (AI, A) => B) = {
    new Storage[AI, A, B] {
      override def create(id: AI, data: A): B = {
        createArg(id, data)
      }

      override def update(b: B): B = b

      override def select(id: AI): B = ???
    }

  }

  implicit def qqSQL[A, B, R <: HList](implicit genA: Generic.Aux[A, R], genB: Generic.Aux[B, (String @@ B) :: R]) =
    instance[String @@ B, A, B] { (id: String @@ B, a: A) =>
      println(2)
      genB.from(id :: genA.to(a))
    }
}

object Conv {
  def apply[PA, A](implicit conv: Conv[PA, A]) =
    conv

  def instance[PA, A](f: PA => A) =
    new Conv[PA, A] {
      override def conv(pa: PA): A =
        f(pa)
    }

  implicit def pa2a[PA, A, PR <: HList, R <: HList](implicit genPA: Generic.Aux[PA, PR], genA: Generic.Aux[A, R], conv: Conv[PR, R]) =
    instance[PA, A] { (pa: PA) =>
      genA.from(conv.conv(genPA.to(pa)))
    }


  implicit def pr2r[PRH, PRT <: HList, PT <: HList](implicit conv: Conv[PRT, PT]) =
    instance[PRH :: PRT, PRH :: PT] { (pr: PRH :: PRT) =>
      pr.head :: conv.conv(pr.tail)
    }

  implicit def hnil2hnil =
    instance[HNil, HNil] { (hnil: HNil) =>
      hnil
    }

  implicit def pr2rSkip[PRH, PRT <: HList, PT <: HList](implicit conv: Conv[PRT, PT]) =
    instance[PRH :: PRT, PT] { (pr: PRH :: PRT) =>
      conv.conv(pr.tail)
    }
}

trait Conv[PA, A] {
  def conv(pa: PA): A
}


trait Storage[AI, A, B] {
  def create(id: AI, data: A): B

  def createApprox[PA](id: AI, data: PA)(implicit conv: Conv[PA, A]): B =
    create(id, conv.conv(data))

  def update(b: B): B

  def select(id: AI): B
}

object StorageApp
  extends App {

  sealed abstract trait T {
    val id: String @@ TR
  }

  case class TId(id: String @@ TR) extends T

  case class TR(id: String @@ TR, t1: Int, t2: Int) extends T

  case class TData(t1: Int, t2: Int)

  case class S(id: String @@ S, s1: String, s2: Int, t: T)

  case class SData(s1: String, d: Double, s2: String, i: Int, t: T)

  println(3)

  val t = Storage[(Int, Int), TR].create(tag[TR]("qq"), (1, 2))

  val s = Storage[(String, Int, T), S].create(tag[S]("qq"), ("", 2, t))

  val s1 = Storage[(String, Int, T), S].create(tag[S]("qq"), ("", 2, TId(tag[TR]("tid"))))

  //val a = the[Conv[(String, Int, T), SData]]

  val s3 = Storage[(String, Int, T), S].createApprox(tag[S]("qq"), SData("", 2.0, "asdasd", 3, TId(tag[TR]("tid")): T))

  case class V(i: Int, s: String)

  case class W(i: Int, d: Double, s: String)

  println(Conv[W, V].conv(W(1, 0.1, "1")))

  println(t, s, s1)

  println(s3)
}
