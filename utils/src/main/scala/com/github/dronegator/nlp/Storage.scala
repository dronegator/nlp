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

trait Storage[AI, A, B] {
  def create(id: AI, data: A): B

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

  println(3)
  val t = Storage[(Int, Int), TR].create(tag[TR]("qq"), (1, 2))

  val s = Storage[(String, Int, T), S].create(tag[S]("qq"), ("", 2, t))

  val s1 = Storage[(String, Int, T), S].create(tag[S]("qq"), ("", 2, TId(tag[TR]("tid"))))

  println(t, s, s1)
}
