package com.github.dronegator.nlp.utils.typeactor

/**
 * Created by cray on 9/18/16.
 */


import akka.actor._

import scala.reflect.ClassTag

case class TypeProps[A](props: Props)(implicit tag: ClassTag[A])

object TypeProps {
  implicit def props2TypeProps[A](props: Props)(implicit tag: ClassTag[A]) =
    TypeProps[A](props)
}
