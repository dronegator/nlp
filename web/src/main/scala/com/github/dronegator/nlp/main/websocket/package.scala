package com.github.dronegator.nlp.main

import java.util.UUID

import com.github.dronegator.nlp.main.phrase.PhraseResponse._
import com.github.dronegator.nlp.main.phrase.Suggest
import enumeratum._
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, JsonFormat}

/**
  * Created by cray on 9/19/16.
  */
package object websocket {

  trait Event

  sealed trait EventType extends EnumEntry

  object EventType extends Enum[EventType] {
    override def values = findValues

    case object Advice extends EventType

    case object Contact extends EventType

    case object Ping extends EventType
  }

  trait EventDestination[A] {
    val destination: A
  }

  case class EventDestinationSession[+E <: Event](destination: String, event: EventEnvelope[E]) extends EventDestination[String]

  object EventEnvelope {
    def apply(event: Advice): EventEnvelope[Advice] = {
      new EventEnvelope(event, EventType.Advice, System.currentTimeMillis(), UUID.randomUUID())
    }

    def apply(event: Contact): EventEnvelope[Contact] = {
      new EventEnvelope(event, EventType.Contact, System.currentTimeMillis(), UUID.randomUUID())
    }

    def apply(event: Ping): EventEnvelope[Ping] = {
      new EventEnvelope(event, EventType.Ping, System.currentTimeMillis(), UUID.randomUUID())
    }
  }

  case class EventEnvelope[+E <: Event](event: E, kind: EventType, time: Long, id: UUID)

  case class Advice(suggest: List[Suggest[String]]) extends Event

  case class Contact() extends Event

  case class Ping(n: Int) extends Event

  object Events extends DefaultJsonProtocol {
    implicit val adviceFormat = jsonFormat1(Advice)

    implicit val contactFormat = jsonFormat0(Contact)

    implicit val pingFormat = jsonFormat1(Ping)

    implicit object EventTypeJsonFormat extends JsonFormat[EventType] {
      def write(eventType: EventType) = JsString(eventType.entryName)

      def read(value: JsValue) = value match {
        case JsString(value) =>
          EventType.withName(value)

        case _ =>
          throw new DeserializationException("String expected")
      }
    }

    implicit object UUIDJsonFormat extends JsonFormat[UUID] {
      def write(uuid: UUID) = JsString(uuid.toString)

      def read(value: JsValue) = value match {
        case JsString(value) =>
          UUID.fromString(value)

        case _ =>
          throw new DeserializationException("String expected")
      }
    }

    implicit val adviceEnvelopFormat = jsonFormat4(EventEnvelope[Advice])

    implicit val contactEnvelopeFormat = jsonFormat4(EventEnvelope[Contact])

    implicit val pingEnvelopeFormat = jsonFormat4(EventEnvelope[Ping])
  }

}
