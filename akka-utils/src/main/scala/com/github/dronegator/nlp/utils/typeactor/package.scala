package com.github.dronegator.nlp.utils

/**
 * Created by cray on 9/20/16.
 */

import akka.actor._

import scala.reflect.ClassTag

package object typeactor {

  implicit class ActorRefFactoryExt[A](system: ActorRefFactory)(implicit tag: ClassTag[A]) {
    def actorOf(props: TypeProps[A]) =
      TypeActorRef[A](system.actorOf(props.props))

    def actorOf(props: TypeProps[A], name: String) =
      TypeActorRef[A](system.actorOf(props.props, name))
  }

  implicit class ActorContextExt[A](system: ActorContext)(implicit tag: ClassTag[A]) {

    def watch(typeActorRef: TypeActorRef[A]) =
      TypeActorRef[A](system.watch(typeActorRef.actorRef))
  }

}
