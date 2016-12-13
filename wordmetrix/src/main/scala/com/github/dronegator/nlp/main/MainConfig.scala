package com.github.dronegator.nlp.main

import java.util.Map.Entry

import com.typesafe.config.{Config, ConfigFactory, ConfigValue}

import scala.collection.JavaConverters._

/**
  * Created by cray on 11/13/16.
  */

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
}
