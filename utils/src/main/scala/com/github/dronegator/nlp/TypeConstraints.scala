package com.github.dronegator.nlp

/*
* I've acquired it from the Gist: https://gist.github.com/OlegIlyenko/8508980
 */

import scala.annotation.implicitNotFound

sealed trait Existence

trait Exists extends Existence

trait NotExists extends Existence

trait IsTypeClassExists[TypeClass, Answer]

object IsTypeClassExists {
  private val evidence: IsTypeClassExists[Any, Any] =
    new Object with IsTypeClassExists[Any, Any]

  implicit def typeClassExistsEv[TypeClass, Answer](implicit a: TypeClass) =
    evidence.asInstanceOf[IsTypeClassExists[TypeClass, Exists]]

  implicit def typeClassNotExistsEv[TypeClass, Answer] =
    evidence.asInstanceOf[IsTypeClassExists[TypeClass, NotExists]]
}

@implicitNotFound("Argument does not satisfy constraints: Not ${T}")
trait Not[T]

object Not {
  private val evidence: Not[Any] = new Object with Not[Any]

  implicit def notEv[T, Answer](implicit a: IsTypeClassExists[T, Answer], ne: Answer =:= NotExists) =
    evidence.asInstanceOf[Not[T]]
}

@implicitNotFound("Argument does not satisfy constraints: ${A} And ${B}")
trait And[A, B]

object And {
  private val evidence: And[Any, Any] = new Object with And[Any, Any]

  implicit def bothExistEv[A, B](implicit a: A, b: B) =
    evidence.asInstanceOf[And[A, B]]
}

@implicitNotFound("Argument does not satisfy constraints: ${A} Or ${B}")
trait Or[A, B]

object Or {
  private val evidence: Or[Any, Any] = new Object with Or[Any, Any]

  implicit def aExistsEv[A, B](implicit a: A) =
    evidence.asInstanceOf[Or[A, B]]

  implicit def bExistsEv[A, B](implicit b: B) =
    evidence.asInstanceOf[Or[A, B]]
}