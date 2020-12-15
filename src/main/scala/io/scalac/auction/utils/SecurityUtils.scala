package io.scalac.auction.utils

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import io.scalac.auction.protocols.{
  AccessControl,
  AccessDenied,
  GeneralProtocol
}

object SecurityUtils {

  def checkAccess[T](
      owner: String,
      message: GeneralProtocol,
      context: ActorContext[GeneralProtocol]
  )(body: GeneralProtocol => Behavior[T]): Behavior[T] = {
    message match {
      case ac: AccessControl if ac.userId != owner =>
        ac.sender ! AccessDenied(context.self)
        Behaviors.same
      case msg: GeneralProtocol =>
        body(msg)
    }
  }

}
