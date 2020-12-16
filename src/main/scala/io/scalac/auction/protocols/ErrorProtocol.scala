package io.scalac.auction.protocols

import akka.actor.typed.ActorRef

sealed trait ErrorProtocol extends GeneralProtocol

final case class InvalidActorState(sender: ActorRef[GeneralProtocol])
    extends ErrorProtocol

final case class InvalidStateTransition(sender: ActorRef[GeneralProtocol])
    extends ErrorProtocol

final case class LotAlreadyExists(
    sender: ActorRef[GeneralProtocol],
    name: String
) extends ErrorProtocol

final case class LotNotFound(sender: ActorRef[GeneralProtocol], name: String)
    extends ErrorProtocol

final case class AccessDenied(sender: ActorRef[GeneralProtocol])
    extends ErrorProtocol
