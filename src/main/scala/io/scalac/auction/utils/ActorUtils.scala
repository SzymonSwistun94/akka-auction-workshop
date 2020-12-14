package io.scalac.auction.utils

import akka.actor.typed.ActorRef

object ActorUtils {
  def  noSender[T]: ActorRef[T] = null
}
