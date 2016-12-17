package com.github.dronegator.nlp

import shapeless._
import shapeless.labelled.FieldType

/**
  * Created by cray on 12/17/16.
  */

trait ToMapRec[L <: HList] {
  def apply(l: L): Map[String, Any]
}

trait LowPriorityToMapRec {
  implicit def hconsToMapRec1[K <: Symbol, V, T <: HList](implicit
                                                          wit: Witness.Aux[K],
                                                          tmrT: ToMapRec[T]
                                                         ): ToMapRec[FieldType[K, V] :: T] =
    new ToMapRec[FieldType[K, V] :: T] {
      def apply(l: FieldType[K, V] :: T): Map[String, Any] =
        tmrT(l.tail) + (wit.value.name -> l.head)
    }
}

object ToMapRec extends LowPriorityToMapRec {
  implicit val hnilToMapRec: ToMapRec[HNil] = new ToMapRec[HNil] {
    def apply(l: HNil): Map[String, Any] = Map.empty
  }

  implicit def hconsToMapRec0[K <: Symbol, V, R <: HList, T <: HList](implicit
                                                                      wit: Witness.Aux[K],
                                                                      gen: LabelledGeneric.Aux[V, R],
                                                                      tmrH: ToMapRec[R],
                                                                      tmrT: ToMapRec[T]
                                                                     ): ToMapRec[FieldType[K, V] :: T] =
    new ToMapRec[FieldType[K, V] :: T] {
      def apply(l: FieldType[K, V] :: T): Map[String, Any] =
        tmrT(l.tail) + (wit.value.name -> tmrH(gen.to(l.head)))
    }
}

object CaseClassToMap {

  implicit class ToMapRecOps[A](val a: A)
    extends AnyVal {
    def toMap[L <: HList](implicit
                          gen: LabelledGeneric.Aux[A, L],
                          tmr: ToMapRec[L]): Map[String, Any] =
      tmr(gen.to(a))
  }

}
