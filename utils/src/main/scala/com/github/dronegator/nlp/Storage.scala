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

//object StorageApp
//  extends App {
//
//  case class T(id: String @@ T, t1: Int, t2: Int)
//
//  case class TData(t1: Int, t2: Int)
//
//  println(3)
//  val t = Storage[(Int, Int), T].create(tag[T]("qq"), (1, 2))
//
//  println(t)
//}
