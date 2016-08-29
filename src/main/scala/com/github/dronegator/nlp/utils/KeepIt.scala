package com.github.dronegator.nlp.utils

import akka.stream.scaladsl.{Sink, Source}

/**
 * Created by cray on 8/29/16.
 */
object KeepIt {
  def plain1[A, B](left: A, right: B) =
    (left, right)

  def plain2[A1, A2, B](left: (A1, A2), right: B) =
    (left._1, left._2, right)

  def plain3[A1, A2, A3, B](left: (A1, A2, A3), right: B) =
    (left._1, left._2, left._3, right)

  def plain4[A1, A2, A3, A4, B](left: (A1, A2, A3, A4), right: B) =
    (left._1, left._2, left._3, left._4, right)

  def plain5[A1, A2, A3, A4, A5, B](left: (A1, A2, A3, A4, A5), right: B) =
    (left._1, left._2, left._3, left._4, left._5, right)

  def plain6[A1, A2, A3, A4, A5, A6, B](left: (A1, A2, A3, A4, A5, A6), right: B) =
    (left._1, left._2, left._3, left._4, left._5, left._6, right)

  def plain7[A1, A2, A3, A4, A5, A6, A7, B](left: (A1, A2, A3, A4, A5, A6, A7), right: B) =
    (left._1, left._2, left._3, left._4, left._5, left._6, left._7, right)

  def plain8[A1, A2, A3, A4, A5, A6, A7, A8, B](left: (A1, A2, A3, A4, A5, A6, A7, A8), right: B) =
    (left._1, left._2, left._3, left._4, left._5, left._6, left._7, left._8, right)

  def plain9[A1, A2, A3, A4, A5, A6, A7, A8, A9, B](left: (A1, A2, A3, A4, A5, A6, A7, A8, A9), right: B) =
    (left._1, left._2, left._3, left._4, left._5, left._6, left._7, left._8, left._9, right)

  def plain10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, B](left: (A1, A2, A3, A4, A5, A6, A7, A8, A9, A10), right: B) =
    (left._1, left._2, left._3, left._4, left._5, left._6, left._7, left._8, left._9, left._10, right)

  def plain2l[A, B1, B2](left: A, right: (B1, B2)) =
    (left, right._1, right._2)

  def plain3l[A, B1, B2, B3](left: A, right: (B1, B2, B3)) =
    (left, right._1, right._2, right._3)

  def plain4l[A, B1, B2, B3, B4](left: A, right: (B1, B2, B3, B4)) =
    (left, right._1, right._2, right._3, right._4)

  def plain5l[A, B1, B2, B3, B4, B5](left: A, right: (B1, B2, B3, B4, B5)) =
    (left, right._1, right._2, right._3, right._4, right._5)

  def plain6l[A, B1, B2, B3, B4, B5, B6](left: A, right: (B1, B2, B3, B4, B5, B6)) =
    (left, right._1, right._2, right._3, right._4, right._5, right._6)

  def plain7l[A, B1, B2, B3, B4, B5, B6, B7](left: A, right: (B1, B2, B3, B4, B5, B6, B7)) =
    (left, right._1, right._2, right._3, right._4, right._5, right._6, right._7)

  def plain8l[A, B1, B2, B3, B4, B5, B6, B7, B8](left: A, right: (B1, B2, B3, B4, B5, B6, B7, B8)) =
    (left, right._1, right._2, right._3, right._4, right._5, right._6, right._7, right._8)

  def plain9l[A, B1, B2, B3, B4, B5, B6, B7, B8, B9](left: A, right: (B1, B2, B3, B4, B5, B6, B7, B8, B9)) =
    (left, right._1, right._2, right._3, right._4, right._5, right._6, right._7, right._8, right._9)

  def plain10l[A, B1, B2, B3, B4, B5, B6, B7, B8, B9, B10](left: A, right: (B1, B2, B3, B4, B5, B6, B7, B8, B9, B10)) =
    (left, right._1, right._2, right._3, right._4, right._5, right._6, right._7, right._8, right._9, right._10)

  val s1 = Source(List(1, 2, 3))
  val s2 = Source(List(1, 2, 3))
  val sink = Sink.ignore

  s1.mergeMat(s2)(KeepIt.plain1).toMat(sink)(KeepIt.plain2)


}
