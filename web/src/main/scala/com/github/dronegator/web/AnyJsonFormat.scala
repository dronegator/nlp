package com.github.dronegator.web

import shapeless.{Path => _}
import spray.json._

/**
  * Created by cray on 3/20/17.
  */
trait AnyJsonFormat extends JsonFormat[Any] {
  def write(x: Any): JsValue = x match {
    case n: Int => JsNumber(n)
    case l: Long => JsNumber(l)
    case d: Double => JsNumber(d)
    case f: Float => JsNumber(f)
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


object AnyJsonFormat {
  implicit object Format extends AnyJsonFormat
}