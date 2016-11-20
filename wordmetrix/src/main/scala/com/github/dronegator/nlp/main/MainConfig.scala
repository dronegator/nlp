package com.github.dronegator.nlp.main

import java.util.Map.Entry

import com.typesafe.config.{Config, ConfigFactory, ConfigValue}

import scala.collection.JavaConverters._

/**
  * Created by cray on 11/13/16.
  */

case class CFG1(allowCommas: Boolean, maxPhrase: Int, language: String, qq: Option[Double])

trait MainConfig[CFG] {
  val config = ConfigFactory.load().getConfig("com.github.dronegator.wordmetrix")

  implicit def configToInt(config: Config, path: String) =
    config.getInt(path)

  implicit def configToBoolean(config: Config, path: String) =
    config.getBoolean(path)

  implicit def configToString(config: Config, path: String) =
    config.getString(path)

  implicit class ConfigToMap[B](config: Config) {
    def getMap[Map[String, B]](key: String)(implicit parse: (Config, String) => B) = {
      (for {
        a: Entry[String, ConfigValue] <- config.getConfig(key).entrySet().asScala
      } yield {
        a.getKey -> parse(config.getConfig(key), a.getKey)
      }).toMap
    }
  }

  //  println(config.getBoolean("allow-commas"))
  //
  //  println(config.getInt("max-phrase"))
  //
  //  println(config.getString("language"))
  //
  //  println(config.get[Int]("max-phrase").toOption)
  //
  //  println(config.get[Option[String]]("language").toOption)
  //
  //  println(config.extract[CFG1].value)
  //  println(config.get[Option[CFG1]]("wordmetrix"))
  //
  ////  val q: CFG1 = config /[CFG1] "wodmetrix"
  ////  println(q)
  //
  ////  val map1 = (for {
  ////   a: Entry[String, ConfigValue] <- config.getConfig("items").entrySet().asScala
  ////  } yield {
  ////    a.getKey -> config.getConfig("items").getInt(a.getKey)
  ////  }).toMap
  //
  //  val map1: Map[String, Int] = config.getMap("items")
  //  println(map1)
  //
  //  val list : Iterable[ConfigObject] = config.getObjectList("decoders").asScala
  //  val map = (for {
  //    item : ConfigObject <- list
  //    entry : Entry[String, ConfigValue] <- item.entrySet().asScala
  //    key = entry.getKey
  //    uri = new URI(entry.getValue.unwrapped().toString)
  //  } yield (key, uri)).toMap
  //
  //  println(map)
}
