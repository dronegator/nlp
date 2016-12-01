package com.github.dronegator.entity

/**
  * Created by cray on 11/28/16.
  */


object Sample {

  case class A(a: Int)

  case class B(b: Int, refA: Entity[A])

  case class C[Q <: Entity[B]](refB: Q)

  val refC: C[EntityRef[B]] = C(EntityRef[B](Id("")))

  val c: C[EntityRecord[B]] = C(refC.refB + B(1, EntityRecord[A](Id(""), A(2))))
}


class Sample {

  import Sample._

  val a1 = EntityRecord(Id("a1"), A(1))
  val a2 = EntityRecord(Id("a2"), A(2))
  val a3 = EntityRecord(Id("a3"), A(3))
  val a4 = EntityRecord(Id("a4"), A(4))

  def getAs() = a1 :: a2 :: a3 :: a4 :: Nil

  def getAsRef() = getAs.map(x => x: EntityRef[A])

  def getA(ref: EntityRef[A]) = getAs().find(_.id == ref.id)

  def getA(ref: Entity[A]) = getAs().find(_.id == ref.id)

  def findA(a: A) = getAs().find(_.data == a)


}
