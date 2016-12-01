package com.github.dronegator

/**
  * Created by cray on 11/28/16.
  */
package object entity {

  case class Id[A](id: String)

  trait Entity[A] {
    val id: Id[A]
  }

  case class EntityRecord[A](id: Id[A], data: A) extends Entity[A]

  case class EntityRef[A](id: Id[A]) extends Entity[A] {
    def +(data: A) =
      EntityRecord(id, data)
  }

  object EntityRef {
    implicit def entityRecordToEntityRef[A](entityRecord: EntityRecord[A]) = EntityRef(entityRecord.id)
  }

}
