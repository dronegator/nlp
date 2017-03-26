package com.github.dronegator.web

import shapeless.labelled.FieldType
import shapeless.ops.hlist.IsHCons
import shapeless.{Path => _, _}

/**
  * Created by cray on 3/26/17.
  */

object CaseClassScheme {
  def apply[A](implicit caseClassScheme: CaseClassScheme[A]) =
    caseClassScheme

  def createCaseClassScheme[A](f: => Map[String, Any]) =
    new CaseClassScheme[A] {
      override def scheme: Map[String, Any] =
        f
    }

  implicit def caseClassSchemeString =
    createCaseClassScheme[String] {
      Map(
        "type" -> "string",
        "example" -> "string"
      )
    }

  implicit def caseClassSchemeField[K <: Symbol, V](implicit witness: Witness.Aux[K],
                                                    caseClassScheme: CaseClassScheme[V]) =
    createCaseClassScheme[FieldType[K, V]] {
      Map(witness.value.name -> caseClassScheme.scheme)
    }


  implicit def caseClassSchemeHNil =
    createCaseClassScheme[HNil] {
      Map[String, Any]()
    }

  implicit def caseClassSchemeHCons[A <: HList, H, T <: HList](implicit isHCons: IsHCons.Aux[A, H, T],
                                                               caseClassSchemeH: CaseClassScheme[H],
                                                               caseClassSchemeT: CaseClassScheme[T]) =
    createCaseClassScheme[A] {

      caseClassSchemeH.scheme ++
        caseClassSchemeT.scheme

    }

  implicit def caseClassSchemeA[A, Repr <: HList](implicit labelledGeneric: LabelledGeneric.Aux[A, Repr],
                                                  caseClassScheme: CaseClassScheme[Repr]) =
    createCaseClassScheme[A] {
      Map(
        "type" -> "object",
        //"required" -> List(),
        "properties" -> caseClassScheme.scheme
      )

    }

}

trait CaseClassScheme[A] {
  def scheme: Map[String, Any]
}
