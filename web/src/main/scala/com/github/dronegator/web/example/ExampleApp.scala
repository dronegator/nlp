package com.github.dronegator.web.example

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives._
import com.github.dronegator.nlp.main.Concurent
import com.github.dronegator.nlp.main.NLPTWebServiceMain._
import com.github.dronegator.web._
import com.github.dronegator.web.example.model._
import com.typesafe.scalalogging.LazyLogging
import fommil.sjs.FamilyFormats
import shapeless.{Path => _, _}
/**
  * Created by cray on 3/20/17.
  */
import spray.json._



object ExampleApp
  extends App
    with Concurent
    with LazyLogging
    with WebAppTrait[MS :: M1 :: M2 :: HNil]
    with SchemaRoute[MS :: M1 :: M2 :: HNil]
    with WebServiceRoute[MS :: M1 :: M2 :: HNil]
    with Description
    with FamilyFormats
    with SprayJsonSupport
    with DefaultJsonProtocol {

  override def description: String = "Yet Another Example of Web Service"

  override def version: String = "0.0.1"

  val qq = the[CaseClassScheme[H1Request]]

  println(qq.scheme)

  case class B(bs: String)

  case class A(b: B, as: String)

  val qa = the[CaseClassScheme[A]]

  println(qa.scheme)

  def modules: MS :: HNil = new MS :: HNil

  def modules1: M1 :: HNil = new M1 :: HNil

  def modules2: M2 :: HNil = new M2 :: HNil

  def moduless: MS :: M1 :: M2 :: HNil = new MS :: new M1 :: new M2 :: HNil

  override def module = new MS :: new M1 :: new M2 :: HNil

  println(schema)

  val route =
    schemaRoute ~
      webServiceRoute

  val bindingFuture = Http().bindAndHandle(route, cfg.host, cfg.port)

  logger.info(s"Server online at http://${cfg.host}:${cfg.port}/\nPress RETURN to stop...")

  Console.readLine() // for the future transformations

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown())

  logger.info(s"Server shutdown")
}

